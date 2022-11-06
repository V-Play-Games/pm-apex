/*
 * Copyright (c) 2008, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package net.vpg.apex.clip;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoftMixingClip implements Clip {
    private final Object control_mutex;
    private final Gain gain = new Gain();
    private final Mute mute = new Mute();
    private final Balance balance = new Balance();
    private final Control[] controls;
    private final List<LineListener> listeners = new ArrayList<>();
    private final SoftMixingMixer mixer;
    private AudioFormat inputFormat;
    private byte[] data;
    private int offset;
    private AudioFormat outputFormat;
    private int framePosition = 0;
    private int loopStart = 0;
    private int loopEnd = -1;
    private int loopCount = 0;
    private int frameSize;
    private int bufferSize;
    private final InputStream stream = new InputStream() {
        @Override
        public int read() throws IOException {
            byte[] b = new byte[1];
            int ret = read(b);
            if (ret < 0)
                return ret;
            return b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            int pos = framePosition * frameSize;
            int loopEndFrame = loopEnd * frameSize;
            if (loopCount == 0 || pos + len < loopEndFrame) { // drain all if no
                len = Math.min(len, bufferSize - pos);
                if (len == 0)
                    return -1;
                System.arraycopy(data, pos, b, off, len);
                framePosition += len / frameSize;
                return len;
            }
            int read = 0;
            while (read < len) {
                if (pos + read >= loopEndFrame) {
                    pos = loopStart * frameSize;
                    if (loopCount != LOOP_CONTINUOUSLY)
                        loopCount--;
                    if (loopCount == 0)
                        break;
                }
                int left = Math.min(len - read, loopEndFrame - pos);
                System.arraycopy(data, pos, b, off + read, left);
                read += left;
            }
            framePosition = pos / frameSize;
            return read;
        }
    };
    private float[] readBuffer;
    private boolean open = false;
    private float leftGain = 1;
    private float rightGain = 1;
    private AudioFloatInputStream inputStream;
    private int out_channels;
    private int in_channels;
    private boolean active = false;

    public SoftMixingClip() {
        this.mixer = new SoftMixingMixer(this);
        this.control_mutex = mixer.control_mutex;
        this.controls = new Control[]{gain, mute, balance};
        calcVolume();
    }

    protected void processAudioLogic(SoftAudioBuffer[] buffers) {
        if (active) {
            int readLen = buffers[0].getSize() * in_channels;
            if (readBuffer == null || readBuffer.length < readLen)
                readBuffer = new float[readLen];
            try {
                int read = inputStream.read(readBuffer);
                if (read == -1) {
                    active = false;
                    return;
                }
                if (read != readLen)
                    Arrays.fill(readBuffer, read, readLen, 0);
            } catch (IOException ignored) {
                //ignore
            }
            fillReadData(buffers[0].getArray(), 0, leftGain);
            if (out_channels != 1)
                fillReadData(buffers[1].getArray(), 1, rightGain);
        }
    }

    private void fillReadData(float[] dest, int ix, float factor) {
        for (int i = 0, len = dest.length; i < len; ix += in_channels) {
            dest[i++] += readBuffer[ix] * factor;
        }
    }

    private void calcVolume() {
        synchronized (control_mutex) {
            double gainValue = Math.pow(10.0, gain.getValue() / 20.0);
            if (mute.getValue())
                gainValue = 0;
            leftGain = (float) gainValue;
            rightGain = (float) gainValue;
            if (outputFormat.getChannels() > 1) {
                // -ve = Left, 0 = Center, +ve = Right
                double balanceValue = balance.getValue();
                if (balanceValue > 0)
                    leftGain *= 1 - balanceValue;
                else
                    rightGain *= 1 + balanceValue;
            }
        }
    }

    private void sendEvent(LineEvent event) {
        listeners.forEach(listener -> listener.update(event));
    }

    public void addLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.add(listener);
        }
    }

    public void removeLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.remove(listener);
        }
    }

    public Control getControl(Control.Type control) {
        return Arrays.stream(controls)
            .filter(c -> c.getType() == control)
            .findFirst()
            .orElse(null);
    }

    public Control[] getControls() {
        return Arrays.copyOf(controls, controls.length);
    }

    public boolean isControlSupported(Control.Type control) {
        return Arrays.stream(controls).anyMatch(c -> c.getType() == control);
    }

    @Override
    public int getFrameLength() {
        return bufferSize / inputFormat.getFrameSize();
    }

    @Override
    public long getMicrosecondLength() {
        return (long) (getFrameLength() * 1000000.0 / getFormat().getSampleRate());
    }

    @Override
    public void loop(int count) {
        LineEvent event;
        synchronized (control_mutex) {
            if (!isOpen() || active)
                return;
            active = true;
            loopCount = count;
            event = new LineEvent(this, LineEvent.Type.START, getLongFramePosition());
        }
        sendEvent(event);
    }

    @Override
    public void open(AudioInputStream stream) throws IOException, LineUnavailableException {
        if (isOpen()) {
            throw new IllegalStateException("Clip is already open with format " + getFormat() + ", and frame length of " + getFrameLength());
        }
        AudioFormat streamFormat = stream.getFormat();
        if (!AudioFloatConverter.isSupported(streamFormat))
            throw new IllegalArgumentException("Invalid format: " + streamFormat.toString());
        byte[] cached = Toolkit.cache(stream);
        open(stream.getFormat(), cached, 0, cached.length);
    }

    @Override
    public void open(AudioFormat format, byte[] data, int offset, int bufferSize) throws LineUnavailableException {
        synchronized (control_mutex) {
            if (isOpen())
                throw new IllegalStateException("Clip is already open with format " + getFormat() + ", and frame length of " + getFrameLength());
            if (!AudioFloatConverter.isSupported(format))
                throw new IllegalArgumentException("Invalid format: " + format.toString());
            Toolkit.validateBuffer(format.getFrameSize(), bufferSize);
            if (data != null)
                this.data = Arrays.copyOf(data, data.length);
            this.offset = offset;
            this.bufferSize = bufferSize;
            this.inputFormat = format;
            this.frameSize = format.getFrameSize();
            loopStart = 0;
            loopEnd = -1;
            mixer.open();
            outputFormat = mixer.getFormat();
            out_channels = outputFormat.getChannels();
            in_channels = format.getChannels();
            open = true;
            inputStream = AudioFloatInputStream.getInputStream(new AudioInputStream(stream, inputFormat, AudioSystem.NOT_SPECIFIED));
        }
    }

    @Override
    public void setLoopPoints(int start, int end) {
        synchronized (control_mutex) {
            if (end == -1)
                end = bufferSize / frameSize;
            if (end < start)
                invalidLoopPoints(start, end);
            if (end * frameSize > bufferSize)
                end = bufferSize / frameSize;
            if (start * frameSize > bufferSize)
                invalidLoopPoints(start, end);
            loopStart = start;
            loopEnd = end;
        }
    }

    private void invalidLoopPoints(int start, int end) {
        throw new IllegalArgumentException("Invalid loop points: " + start + " - " + end);
    }

    @Override
    public int available() {
        return 0;
    }

    @Override
    public void drain() {
    }

    @Override
    public void flush() {
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public AudioFormat getFormat() {
        return inputFormat;
    }

    @Override
    public int getFramePosition() {
        return framePosition;
    }

    @Override
    public void setFramePosition(int frames) {
        synchronized (control_mutex) {
            framePosition = frames;
        }
    }

    @Override
    public float getLevel() {
        return AudioSystem.NOT_SPECIFIED;
    }

    @Override
    public long getLongFramePosition() {
        return getFramePosition();
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) (getFramePosition() * 1000000.0 / getFormat().getSampleRate());
    }

    @Override
    public void setMicrosecondPosition(long microseconds) {
        setFramePosition((int) (microseconds * getFormat().getSampleRate() / 1000000.0));
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public boolean isRunning() {
        return active;
    }

    @Override
    public void start() {
        LineEvent event;
        synchronized (control_mutex) {
            if (!isOpen() || active)
                return;
            active = true;
            loopCount = 0;
            event = new LineEvent(this, LineEvent.Type.START, getLongFramePosition());
        }
        sendEvent(event);
    }

    @Override
    public void stop() {
        LineEvent event;
        synchronized (control_mutex) {
            if (!isOpen() || !active)
                return;
            active = false;
            event = new LineEvent(this, LineEvent.Type.STOP, getLongFramePosition());
        }
        sendEvent(event);
    }

    @Override
    public void close() {
        LineEvent event;
        synchronized (control_mutex) {
            if (!isOpen())
                return;
            stop();
            event = new LineEvent(this, LineEvent.Type.CLOSE, getLongFramePosition());
            data = null;
            offset = 0;
            bufferSize = 0;
            inputFormat = null;
            frameSize = 0;
            loopStart = 0;
            loopEnd = -1;
            framePosition = 0;
            active = false;
            loopCount = 0;
            mixer.close();
            outputFormat = null;
            out_channels = 0;
            in_channels = 0;
            open = false;
        }
        sendEvent(event);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public Line.Info getLineInfo() {
        return null;
    }

    @Override
    public void open() throws LineUnavailableException {
        if (data == null) {
            throw new IllegalArgumentException("Illegal call to open() in interface Clip");
        }
        open(inputFormat, data, offset, bufferSize);
    }

    private class Gain extends FloatControl {
        private Gain() {
            super(FloatControl.Type.MASTER_GAIN, -80f, 6.0206f, 80f / 128.0f,
                -1, 0.0f, "dB", "Minimum", "", "Maximum");
        }

        @Override
        public void setValue(float newValue) {
            super.setValue(newValue);
            calcVolume();
        }
    }

    private class Mute extends BooleanControl {
        private Mute() {
            super(BooleanControl.Type.MUTE, false, "True", "False");
        }

        @Override
        public void setValue(boolean newValue) {
            super.setValue(newValue);
            calcVolume();
        }
    }

    private class Balance extends FloatControl {
        private Balance() {
            super(FloatControl.Type.BALANCE, -1.0f, 1.0f, 1.0f / 128.0f, -1,
                0.0f, "", "Left", "Center", "Right");
        }

        @Override
        public void setValue(float newValue) {
            super.setValue(newValue);
            calcVolume();
        }
    }
}
