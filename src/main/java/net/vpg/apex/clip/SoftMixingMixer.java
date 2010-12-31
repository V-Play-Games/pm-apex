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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static javax.sound.sampled.AudioFormat.Encoding.*;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;

/**
 * Software audio mixer.
 *
 * @author Karl Helgason
 */
public class SoftMixingMixer {
    public static final int CHANNEL_LEFT = 0;
    public static final int CHANNEL_RIGHT = 1;
    protected final float controlRate = 147f;
    protected final long latency = 100000; // 100 milliseconds
    protected final List<LineListener> listeners = new ArrayList<>();
    protected final Line.Info[] sourceLineInfo;
    protected final List<SoftMixingClip> openLines = new ArrayList<>();
    final Object control_mutex = this;
    public boolean implicitOpen = false;
    protected boolean open = false;
    protected AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
    protected SourceDataLine sourceDataLine = null;
    protected SoftAudioPusher pusher = null;
    protected AudioInputStream pusherStream = null;

    public SoftMixingMixer() {
        sourceLineInfo = new Line.Info[2];

        ArrayList<AudioFormat> formats = new ArrayList<>();
        for (int channels = 1; channels <= 2; channels++) {
            formats.add(new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, 8, channels, channels, NOT_SPECIFIED, false));
            formats.add(new AudioFormat(PCM_UNSIGNED, NOT_SPECIFIED, 8, channels, channels, NOT_SPECIFIED, false));
            for (int bits = 16; bits < 32; bits += 8) {
                formats.add(new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, bits, channels, channels * bits / 8, NOT_SPECIFIED, false));
                formats.add(new AudioFormat(PCM_UNSIGNED, NOT_SPECIFIED, bits, channels, channels * bits / 8, NOT_SPECIFIED, false));
                formats.add(new AudioFormat(PCM_SIGNED, NOT_SPECIFIED, bits, channels, channels * bits / 8, NOT_SPECIFIED, true));
                formats.add(new AudioFormat(PCM_UNSIGNED, NOT_SPECIFIED, bits, channels, channels * bits / 8, NOT_SPECIFIED, true));
            }
            formats.add(new AudioFormat(PCM_FLOAT, NOT_SPECIFIED, 32, channels, channels * 4, NOT_SPECIFIED, false));
            formats.add(new AudioFormat(PCM_FLOAT, NOT_SPECIFIED, 32, channels, channels * 4, NOT_SPECIFIED, true));
            formats.add(new AudioFormat(PCM_FLOAT, NOT_SPECIFIED, 64, channels, channels * 8, NOT_SPECIFIED, false));
            formats.add(new AudioFormat(PCM_FLOAT, NOT_SPECIFIED, 64, channels, channels * 8, NOT_SPECIFIED, true));
        }
        AudioFormat[] formats_array = formats.toArray(new AudioFormat[0]);
        sourceLineInfo[0] = new DataLine.Info(SourceDataLine.class, formats_array, NOT_SPECIFIED, NOT_SPECIFIED);
        sourceLineInfo[1] = new DataLine.Info(Clip.class, formats_array, NOT_SPECIFIED, NOT_SPECIFIED);
    }

    public void close() {
        if (!isOpen())
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
//            openLines.forEach(Line::close);
            if (sourceDataLine != null) {
                sourceDataLine.drain();
                sourceDataLine.close();
                sourceDataLine = null;
            }
        }
    }

    public boolean isOpen() {
        synchronized (control_mutex) {
            return open;
        }
    }

    public void open() throws LineUnavailableException {
        open(null);
    }

    public void open(SourceDataLine line) throws LineUnavailableException {
        if (isOpen()) {
            implicitOpen = false;
            return;
        }
        synchronized (control_mutex) {
            try {
                if (line != null) {
                    format = line.getFormat();
                } else {
                    Mixer defaultMixer = AudioSystem.getMixer(null);
                    if (defaultMixer != null) {
                        // Search for suitable line
                        line = Arrays.stream(defaultMixer.getSourceLineInfo())
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
                                    try {
                                        return (SourceDataLine) defaultMixer.getLine(info);
                                    } catch (LineUnavailableException e) {
                                        throw new RuntimeException(e);
                                    }
                                }))
                            .filter(Optional::isPresent)
                            .findFirst()
                            .map(Optional::get)
                            .orElse(null);
                    }
                    if (line == null)
                        line = AudioSystem.getSourceDataLine(format);
                    assert line != null : new IllegalArgumentException("No line matching this mixer is supported.");
                }
                AudioInputStream ais = openStream(getFormat());

                if (!line.isOpen()) {
                    int bufferSize = getFormat().getFrameSize() * (int) (getFormat().getFrameRate() * (latency / 1000000f));
                    line.open(getFormat(), bufferSize);
                    sourceDataLine = line;
                }
                if (!line.isActive())
                    line.start();

                pusher = new SoftAudioPusher(line, ais, ais.available());
                pusherStream = ais;
                pusher.start();
            } catch (LineUnavailableException | IOException e) {
                if (isOpen())
                    close();
                if (e instanceof LineUnavailableException)
                    throw (LineUnavailableException) e;
            }
        }
    }

    public AudioInputStream openStream(AudioFormat targetFormat) throws LineUnavailableException {
        if (isOpen())
            throw new LineUnavailableException("Mixer is already open");
//        LineEvent event;
        AudioInputStream inputStream;
        synchronized (control_mutex) {
            open = true;
            implicitOpen = false;
            if (targetFormat != null)
                format = targetFormat;
//            event = new LineEvent(this, LineEvent.Type.OPEN, NOT_SPECIFIED);
            inputStream = getInputStream();
        }
//        sendEvent(event);
        return inputStream;
    }

    public AudioInputStream getInputStream() {
        int channels = format.getChannels();
        int bufferSize = (int) (format.getSampleRate() / this.getControlRate());
        SoftAudioBuffer[] buffers = new SoftAudioBuffer[16];
        for (int i = 0; i < buffers.length; i++) {
            buffers[i] = new SoftAudioBuffer(bufferSize, format);
        }

        float sampleRate = format.getSampleRate();
        InputStream in = new InputStream() {
            final byte[] buffer = new byte[bufferSize * (format.getSampleSizeInBits() / 8) * channels];
            final byte[] single = new byte[1];
            int bufferPos = 0;

            public void fillBuffer() {
                for (SoftAudioBuffer buffer : buffers) {
                    buffer.clear();
                }
                openLines.forEach(openLine -> openLine.processAudioLogic(buffers));
                for (int i = 0; i < channels; i++)
                    buffers[i].get(buffer, i);
                bufferPos = 0;
            }

            @Override
            public int read(byte[] b, int off, int len) {
                int offsetLen = off + len;
                while (off < offsetLen) {
                    if (buffer.length == bufferPos) {
                        fillBuffer();
                    } else {
                        while (off < offsetLen && bufferPos < buffer.length) {
                            b[off++] = buffer[bufferPos++];
                        }
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
        return new AudioInputStream(in, this.getFormat(), AudioSystem.NOT_SPECIFIED);
    }

    public void openLine(SoftMixingClip line) {
        synchronized (control_mutex) {
            openLines.add(line);
        }
    }

    public void closeLine(SoftMixingClip line) {
        synchronized (control_mutex) {
            openLines.remove(line);
        }
    }

    public AudioFormat getFormat() {
        synchronized (control_mutex) {
            return format;
        }
    }

    float getControlRate() {
        return controlRate;
    }
}
