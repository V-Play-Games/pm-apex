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
import java.util.Optional;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

public class SoftMixingMixer {
    protected final SoftMixingClip clip;
    protected final Object control_mutex = new Object();
    protected boolean open = false;
    protected AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
    protected SourceDataLine sourceDataLine = null;
    protected SoftAudioPusher pusher = null;
    protected AudioInputStream pusherStream = null;

    public SoftMixingMixer(SoftMixingClip clip) {
        this.clip = clip;
    }

    public void close() {
        if (!open)
            return;
//        sendEvent(new LineEvent(this, LineEvent.Type.CLOSE, NOT_SPECIFIED));

        if (pusher != null) {
            SoftAudioPusher tempPusher;
            AudioInputStream tempPusherStream;
            synchronized (control_mutex) {
                tempPusher = pusher;
                tempPusherStream = pusherStream;
                pusher = null;
                pusherStream = null;
            }

            if (tempPusher != null) {
                // Pusher must not be closed synchronized against control_mutex
                // this may result in synchronized conflict between the pusher and
                // current thread.
                tempPusher.stop();
                Util.run(tempPusherStream::close);
            }
        }

        synchronized (control_mutex) {
            open = false;
            if (sourceDataLine != null) {
                sourceDataLine.drain();
                sourceDataLine.close();
            }
        }
    }

    public void open() throws LineUnavailableException {
        if (open) {
            return;
        }
        synchronized (control_mutex) {
            try {
                Mixer defaultMixer = AudioSystem.getMixer(null);
                if (defaultMixer != null && sourceDataLine == null) {
                    // Search for suitable line
                    sourceDataLine = Arrays.stream(defaultMixer.getSourceLineInfo())
                        .filter(lineInfo -> lineInfo.getLineClass() == SourceDataLine.class)
                        .map(SourceDataLine.Info.class::cast)
                        .map(info -> Arrays.stream(info.getFormats())
                            .filter(format -> (format.getChannels() == 2 || format.getChannels() == NOT_SPECIFIED)
                                && (format.getEncoding().equals(PCM_SIGNED) || format.getEncoding().equals(PCM_UNSIGNED))
                                && (format.getSampleRate() == NOT_SPECIFIED || format.getSampleRate() == 48000.0)
                                && (format.getSampleSizeInBits() == NOT_SPECIFIED || format.getSampleSizeInBits() == 16))
                            .findFirst()
                            .map(idealFormat -> {
                                float idealRate = idealFormat.getSampleRate();
                                int idealChannels = idealFormat.getChannels();
                                int idealBits = idealFormat.getSampleSizeInBits();
                                format = new AudioFormat(
                                    idealRate == NOT_SPECIFIED ? 48000.0F : idealRate,
                                    idealBits == NOT_SPECIFIED ? 16 : idealBits,
                                    idealChannels == NOT_SPECIFIED ? 2 : idealChannels,
                                    idealFormat.getEncoding().equals(PCM_SIGNED),
                                    idealFormat.isBigEndian()
                                );
                                return Util.get(() -> (SourceDataLine) defaultMixer.getLine(info));
                            }))
                        .filter(Optional::isPresent)
                        .findFirst()
                        .flatMap(x -> x)
                        .orElseGet(() -> Util.get(() -> AudioSystem.getSourceDataLine(format)));
                }
                if (sourceDataLine == null) {
                    throw new IllegalArgumentException("No line matching this mixer is supported.");
                }
                if (!sourceDataLine.isOpen()) {
                    int bufferSize = (int) (format.getFrameSize() * format.getFrameRate() * 0.1);
                    sourceDataLine.open(format, bufferSize);
                }
                if (!sourceDataLine.isActive()) {
                    sourceDataLine.start();
                }
                pusherStream = getInputStream();
                pusher = new SoftAudioPusher(sourceDataLine, pusherStream, pusherStream.available());
                pusher.start();

                open = true;
            } catch (LineUnavailableException | IOException e) {
                if (e instanceof LineUnavailableException)
                    throw (LineUnavailableException) e;
            }
        }
    }

    public AudioInputStream getInputStream() {
        int channels = format.getChannels();
        int bufferSize = (int) (format.getSampleRate() * 0.1);
        SoftAudioBuffer[] buffers = new SoftAudioBuffer[channels];
        for (int i = 0; i < channels; i++) {
            buffers[i] = new SoftAudioBuffer(bufferSize, format);
        }

        InputStream in = new InputStream() {
            final byte[] buffer = new byte[bufferSize * (format.getSampleSizeInBits() / 8) * channels];
            final byte[] single = new byte[1];
            int bufferPos = 0;

            public void fillBuffer() {
                for (SoftAudioBuffer buffer : buffers) {
                    buffer.clear();
                }
                clip.processAudioLogic(buffers);
                for (int i = 0; i < channels; i++)
                    buffers[i].get(buffer, i);
                bufferPos = 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int offsetLen = off + len;
                while (off < offsetLen) {
                    b[off++] = buffer[bufferPos++];
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
        return new AudioInputStream(in, format, AudioSystem.NOT_SPECIFIED);
    }

    public AudioFormat getFormat() {
        return format;
    }
}
