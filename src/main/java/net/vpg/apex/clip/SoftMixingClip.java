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
import java.util.Arrays;

/**
 * Clip implementation for the SoftMixingMixer.
 *
 * @author Karl Helgason
 */
public class SoftMixingClip extends SoftMixingDataLine implements Clip {
    protected AudioFormat inputFormat;
    protected byte[] data;
    protected int offset;
    protected AudioFormat outputFormat;
    protected int framePosition = 0;
    protected int loopStart = 0;
    protected int loopEnd = -1;
    protected int loopCount = 0;
    protected InputStream stream = new InputStream() {
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
            if (loopCount != 0) {
                int loopEndFrame = loopEnd * frameSize;
                if (pos + len >= loopEndFrame) {
                    int offend = off + len;
                    int o = off;
                    while (off <= offend) {
                        if (pos + off >= loopEndFrame) {
                            pos = loopStart * frameSize;
                            if (loopCount != LOOP_CONTINUOUSLY)
                                loopCount--;
                            break;
                        }
                        len = offend - off;
                        int left = loopEndFrame - pos;
                        if (len > left)
                            len = left;
                        System.arraycopy(data, pos, b, off, len);
                        off += len;
                    }
                    if (loopCount == 0) {
                        len = offend - off;
                        int left = loopEndFrame - pos;
                        if (len > left)
                            len = left;
                        System.arraycopy(data, pos, b, off, len);
                        off += len;
                    }
                    framePosition = pos / frameSize;
                    return off - o;
                }
            }
            int left = bufferSize - pos;
            if (left == 0)
                return -1;
            if (len > left)
                len = left;
            System.arraycopy(data, pos, b, off, len);
            framePosition += len / frameSize;
            return len;
        }
    };

    public SoftMixingClip(SoftMixingMixer mixer, Info info) {
        super(mixer, info);
    }

    @Override
    protected void processControlLogic() {
        if (inputStream == null) {
            inputStream = AudioFloatInputStream.getInputStream(new AudioInputStream(stream, inputFormat, AudioSystem.NOT_SPECIFIED));
        }
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
            if (!mixer.isOpen()) {
                mixer.open();
                mixer.implicitOpen = true;
            }
            outputFormat = mixer.getFormat();
            out_channels = outputFormat.getChannels();
            in_channels = format.getChannels();
            stereo = in_channels == 2;
            open = true;
            mixer.openLine(this);
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
        synchronized (control_mutex) {
            return framePosition;
        }
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
        synchronized (control_mutex) {
            return active;
        }
    }

    @Override
    public boolean isRunning() {
        synchronized (control_mutex) {
            return active;
        }
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
            if (!mixer.isOpen()) {
                mixer.close();
                mixer.implicitOpen = false;
            }
            outputFormat = null;
            out_channels = 0;
            in_channels = 0;
            open = false;
            mixer.closeLine(this);
        }
        sendEvent(event);
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void open() throws LineUnavailableException {
        if (data == null) {
            throw new IllegalArgumentException("Illegal call to open() in interface Clip");
        }
        open(inputFormat, data, offset, bufferSize);
    }
}
