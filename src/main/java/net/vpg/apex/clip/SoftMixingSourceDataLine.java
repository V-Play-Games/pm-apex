/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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

/**
 * SourceDataLine implementation for the SoftMixingMixer.
 *
 * @author Karl Helgason
 */
public class SoftMixingSourceDataLine extends SoftMixingDataLine implements SourceDataLine {
    private final Object buffer_mutex = new Object();
    private AudioFormat format = new AudioFormat(44100.0f, 16, 2, true, false);
    private byte[] cycling_buffer;
    private int cycling_read_pos = 0;
    private int cycling_write_pos = 0;
    private int cycling_avail = 0;
    private long cycling_framePos = 0;

    public SoftMixingSourceDataLine(SoftMixingMixer mixer, Info info) {
        super(mixer, info);
    }

    @Override
    public int write(byte[] b, int off, int len) {
        if (!isOpen())
            return 0;
        if (len % frameSize != 0)
            throw new IllegalArgumentException("Number of bytes does not represent an integral number of sample frames.");
        if (off < 0)
            throw new ArrayIndexOutOfBoundsException(off);
        if ((long) off + (long) len > (long) b.length)
            throw new ArrayIndexOutOfBoundsException(b.length);

        int l = 0;
        while (l != len) {
            synchronized (buffer_mutex) {
                while (l != len) {
                    if (cycling_avail == cycling_buffer.length)
                        break;
                    cycling_buffer[cycling_write_pos++] = b[off++];
                    l++;
                    cycling_avail++;
                    if (cycling_write_pos == cycling_buffer.length)
                        cycling_write_pos = 0;
                }
                if (l == len)
                    return l;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                return l;
            }
            if (!isRunning())
                return l;
        }
        return l;
    }

    @Override
    protected void processControlLogic() {
    }

    @Override
    public void open() throws LineUnavailableException {
        open(format);
    }

    @Override
    public void open(AudioFormat format) throws LineUnavailableException {
        if (bufferSize == -1)
            bufferSize = (int) (format.getFrameRate() / 2) * format.getFrameSize();
        open(format, bufferSize);
    }

    @Override
    public void open(AudioFormat format, int bufferSize) throws LineUnavailableException {
        LineEvent event;
        bufferSize = Math.max(bufferSize, format.getFrameSize() * 32);
        synchronized (control_mutex) {
            if (isOpen())
                throw new IllegalStateException("Line is already open with format " + getFormat() + " and bufferSize " + getBufferSize());
            if (!mixer.isOpen()) {
                mixer.open();
                mixer.implicitOpen = true;
            }
            event = new LineEvent(this, LineEvent.Type.OPEN, 0);

            this.bufferSize = bufferSize - bufferSize % format.getFrameSize();
            this.format = format;
            this.frameSize = format.getFrameSize();
            AudioFormat outputFormat = mixer.getFormat();
            out_channels = outputFormat.getChannels();
            in_channels = format.getChannels();
            stereo = in_channels == 2;
            open = true;
            mixer.openLine(this);

            cycling_buffer = new byte[frameSize * bufferSize];
            cycling_read_pos = 0;
            cycling_write_pos = 0;
            cycling_avail = 0;
            cycling_framePos = 0;
            InputStream stream = new InputStream() {
                @Override
                public int read() throws IOException {
                    byte[] b = new byte[1];
                    return read(b) < 0 ? -1 : b[0] & 0xFF;
                }

                @Override
                public int available() {
                    synchronized (buffer_mutex) {
                        return cycling_avail;
                    }
                }

                @Override
                public int read(byte[] b, int off, int len) {
                    synchronized (buffer_mutex) {
                        len = Math.min(len, cycling_avail);
                        for (int i = 0; i < len; i++) {
                            b[off++] = cycling_buffer[cycling_read_pos];
                            cycling_read_pos++;
                            if (cycling_read_pos == cycling_buffer.length)
                                cycling_read_pos = 0;
                        }
                        cycling_avail -= len;
                        cycling_framePos += len / frameSize;
                    }
                    return len;
                }
            };
            inputStream = AudioFloatInputStream.getInputStream(new AudioInputStream(stream, format, AudioSystem.NOT_SPECIFIED));

            if (Math.abs(format.getSampleRate() - outputFormat.getSampleRate()) > 0.000001)
                inputStream = new AudioFloatInputStreamResampler(inputStream, outputFormat);
        }
        sendEvent(event);
    }

    @Override
    public int available() {
        synchronized (buffer_mutex) {
            return cycling_buffer.length - cycling_avail;
        }
    }

    @Override
    public void drain() {
        while (true) {
            synchronized (buffer_mutex) {
                if (cycling_avail == 0)
                    return;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    @Override
    public void flush() {
        synchronized (buffer_mutex) {
            cycling_read_pos = 0;
            cycling_write_pos = 0;
            cycling_avail = 0;
        }
    }

    @Override
    public int getBufferSize() {
        synchronized (control_mutex) {
            return bufferSize;
        }
    }

    @Override
    public AudioFormat getFormat() {
        synchronized (control_mutex) {
            return format;
        }
    }

    @Override
    public int getFramePosition() {
        return (int) getLongFramePosition();
    }

    @Override
    public float getLevel() {
        return AudioSystem.NOT_SPECIFIED;
    }

    @Override
    public long getLongFramePosition() {
        synchronized (buffer_mutex) {
            return cycling_framePos;
        }
    }

    @Override
    public long getMicrosecondPosition() {
        return (long) (getLongFramePosition() * 1000000.0 / getFormat().getSampleRate());
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
        if (!isOpen() || isActive()) {
            return;
        }
        LineEvent event;
        synchronized (control_mutex) {
            active = true;
            event = new LineEvent(this, LineEvent.Type.START, getLongFramePosition());
        }
        sendEvent(event);
    }

    @Override
    public void stop() {
        if (!isOpen() || !isActive()) {
            return;
        }
        LineEvent event;
        synchronized (control_mutex) {
            active = false;
            event = new LineEvent(this, LineEvent.Type.STOP, getLongFramePosition());
        }
        sendEvent(event);
    }

    @Override
    public void close() {
        if (!isOpen())
            return;
        LineEvent event;
        synchronized (control_mutex) {
            stop();
            event = new LineEvent(this, LineEvent.Type.CLOSE, getLongFramePosition());
            open = false;
            mixer.closeLine(this);
        }
        sendEvent(event);
    }

    @Override
    public boolean isOpen() {
        synchronized (control_mutex) {
            return open;
        }
    }
}
