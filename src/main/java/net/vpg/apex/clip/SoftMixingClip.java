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

import net.vpg.apex.Util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SoftMixingClip implements Clip {
    private final Object control_mutex = new Object();
    private final List<LineListener> listeners = new ArrayList<>();
    protected SourceDataLine sourceDataLine;
    protected SoftAudioPusher pusher;
    private AudioFormat format;
    private byte[] data;
    private int framePosition = 0;
    private int loopStart = 0;
    private int loopEnd = -1;
    private int loopCount = 0;
    private int frameSize;
    private int bufferSize;
    private class LoopingArrayReaderStream extends InputStream {
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
    }
    private boolean open = false;
    private boolean active = false;

    private void sendEvent(LineEvent event) {
        listeners.forEach(listener -> listener.update(event));
    }

    @Override
    public void addLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.remove(listener);
        }
    }

    @Override
    public Control getControl(Control.Type control) {
        return null;
    }

    @Override
    public Control[] getControls() {
        return null;
    }

    @Override
    public boolean isControlSupported(Control.Type control) {
        return false;
    }

    @Override
    public int getFrameLength() {
        return bufferSize / format.getFrameSize();
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
    public void open(AudioInputStream stream) throws IOException {
        byte[] cached = Toolkit.cache(stream);
        open(stream.getFormat(), cached, 0, cached.length);
    }

    @Override
    public void open(AudioFormat format, byte[] data, int offset, int bufferSize) {
        synchronized (control_mutex) {
            Toolkit.validateBuffer(format.getFrameSize(), bufferSize);
            this.data = Arrays.copyOfRange(data, offset, offset + bufferSize);
            this.bufferSize = bufferSize;
            open = true;
            loopStart = 0;
            loopEnd = -1;
            framePosition = 0;
            loopCount = 0;
            if (this.format != format) {
                this.format = format;
                frameSize = format.getFrameSize();
            }
            openSourceDataLine();
        }
    }

    @Override
    public void setLoopPoints(int start, int end) {
        synchronized (control_mutex) {
            if (end == AudioSystem.NOT_SPECIFIED || end * frameSize > bufferSize)
                end = bufferSize / frameSize;
            if (end < start || start * frameSize > bufferSize)
                throw new IllegalArgumentException("Invalid loop points: " + start + " - " + end);
            loopStart = start;
            loopEnd = end;
        }
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
        return format;
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
            format = null;
            active = false;
            bufferSize = 0;
            frameSize = 0;
            loopStart = 0;
            loopEnd = -1;
            framePosition = 0;
            loopCount = 0;
            pusher.stop();
            sourceDataLine.drain();
            sourceDataLine.close();
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
        return new DataLine.Info(SoftMixingClip.class, format);
    }

    @Override
    public void open() {
        if (data == null) {
            throw new IllegalArgumentException("Illegal call to open() in interface Clip");
        }
        open(format, data, 0, bufferSize);
    }

    private void openSourceDataLine() {
        if (sourceDataLine == null) {
            // Search for suitable line
            Mixer defaultMixer = AudioSystem.getMixer(null);
            sourceDataLine = Arrays.stream(defaultMixer.getSourceLineInfo())
                .filter(lineInfo -> lineInfo.getLineClass() == SourceDataLine.class)
                .map(SourceDataLine.Info.class::cast)
                .map(info -> Util.get(() -> (SourceDataLine) defaultMixer.getLine(info)))
                .findFirst()
                .orElseGet(() -> Util.get(() -> AudioSystem.getSourceDataLine(format)));
            pusher = new SoftAudioPusher(sourceDataLine, new LoopingArrayReaderStream());
        }
        if (!sourceDataLine.isOpen()) {
            int bufferSize = (int) (format.getFrameSize() * format.getFrameRate() * 0.1);
            Util.run(() -> sourceDataLine.open(format, bufferSize));
        }
        if (!sourceDataLine.isActive()) {
            sourceDataLine.start();
        }
        pusher.start();
    }
}
