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

import net.vpg.apex.Util;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class SoftMixingMixer {
    protected final SoftMixingClip clip;
    protected final Object control_mutex = new Object();
    protected boolean open = false;
    protected SourceDataLine sourceDataLine;
    protected SoftAudioPusher pusher;
    protected AudioInputStream pusherStream;

    public SoftMixingMixer(SoftMixingClip clip) {
        this.clip = clip;
    }

    public void close() {
        if (!open)
            return;
        synchronized (control_mutex) {
            sourceDataLine.drain();
            sourceDataLine.close();
            pusher.stop();
            Util.run(pusherStream::close);
            pusherStream = null;
            open = false;
        }
    }

    public void open(AudioFormat format) {
        if (open)
            return;
        synchronized (control_mutex) {
            openSourceDataLine(format);
            openStreams(format);
            open = true;
        }
    }

    private void openSourceDataLine(AudioFormat format) {
        if (sourceDataLine == null) {
            // Search for suitable line
            Mixer defaultMixer = AudioSystem.getMixer(null);
            sourceDataLine = Arrays.stream(defaultMixer.getSourceLineInfo())
                .filter(lineInfo -> lineInfo.getLineClass() == SourceDataLine.class)
                .map(SourceDataLine.Info.class::cast)
                .map(info -> Util.get(() -> (SourceDataLine) defaultMixer.getLine(info)))
                .findFirst()
                .orElseGet(() -> Util.get(() -> AudioSystem.getSourceDataLine(format)));
            pusher = new SoftAudioPusher(sourceDataLine);
        }
        if (!sourceDataLine.isOpen()) {
            int bufferSize = (int) (format.getFrameSize() * format.getFrameRate() * 0.1);
            Util.run(() -> sourceDataLine.open(format, bufferSize));
        }
        if (!sourceDataLine.isActive()) {
            sourceDataLine.start();
        }
    }

    private void openStreams(AudioFormat format) {
        int channels = format.getChannels();
        int bufferSize = (int) (format.getSampleRate() * 0.1);
        SoftAudioBuffer[] buffers = new SoftAudioBuffer[channels];
        for (int i = 0; i < channels; i++) {
            buffers[i] = new SoftAudioBuffer(bufferSize, format);
        }
        int sampleBufferSize = bufferSize * (format.getSampleSizeInBits() / 8) * channels;

        InputStream in = new InputStream() {
            final byte[] buffer = new byte[sampleBufferSize];
            final byte[] single = new byte[1];
            int bufferPos = 0;

            public void fillBuffer() {
                for (SoftAudioBuffer buffer : buffers) {
                    buffer.clear();
                }
                clip.processAudioLogic(buffers);
                for (int i = 0; i < buffers.length; i++)
                    buffers[i].get(buffer, i);
                bufferPos = 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int read = 0;
                while (read < len) {
                    read = Math.min(len - read, buffer.length - bufferPos);
                    System.arraycopy(buffer, bufferPos, b, off, read);
                    bufferPos += read;
                    if (bufferPos == buffer.length) {
                        fillBuffer();
                    }
                }
                return len;
            }

            @Override
            public int read() throws IOException {
                return read(single) == -1 ? -1 : single[0] & 0xFF;
            }

            @Override
            public int available() {
                return buffer.length - bufferPos;
            }
        };
        pusherStream = new AudioInputStream(in, format, AudioSystem.NOT_SPECIFIED);
        pusher.setStream(pusherStream, sampleBufferSize);
        pusher.start();
    }

    public AudioFormat getFormat() {
        return sourceDataLine.getFormat();
    }
}
