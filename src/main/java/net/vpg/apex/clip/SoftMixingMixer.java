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
import javax.sound.sampled.Control.Type;
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
public class SoftMixingMixer implements Mixer {
    public static final int CHANNEL_LEFT = 0;
    public static final int CHANNEL_RIGHT = 1;
    public static final int CHANNEL_EFFECT1 = 2;
    public static final int CHANNEL_EFFECT2 = 3;
    public static final int CHANNEL_EFFECT3 = 4;
    public static final int CHANNEL_EFFECT4 = 5;
    public static final int CHANNEL_LEFT_DRY = 10;
    public static final int CHANNEL_RIGHT_DRY = 11;
    public static final int CHANNEL_SCRATCH1 = 12;
    public static final int CHANNEL_SCRATCH2 = 13;
    public static final int CHANNEL_MIXER_LEFT = 14;
    public static final int CHANNEL_MIXER_RIGHT = 15;
    static final String INFO_NAME = "Gervill Sound Mixer";
    static final String INFO_VENDOR = "OpenJDK Proposal";
    static final String INFO_DESCRIPTION = "Software Sound Mixer";
    static final String INFO_VERSION = "1.0";
    static final Info info = new Info(INFO_NAME, INFO_VENDOR, INFO_DESCRIPTION, INFO_VERSION) {
    };
    protected final float controlRate = 147f;
    protected final long latency = 100000; // 100 milliseconds
    protected final List<LineListener> listeners = new ArrayList<>();
    protected final Line.Info[] sourceLineInfo;
    protected final List<SoftMixingDataLine> openLines = new ArrayList<>();
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

    @Override
    public Line getLine(Line.Info info) {
        if (!isLineSupported(info))
            throw new IllegalArgumentException("Line unsupported: " + info);
        if (info.getLineClass() == SourceDataLine.class) {
            return new SoftMixingSourceDataLine(this, (DataLine.Info) info);
        }
        if (info.getLineClass() == Clip.class) {
            return new SoftMixingClip(this, (DataLine.Info) info);
        }
        throw new IllegalArgumentException("Line unsupported: " + info);
    }

    @Override
    public int getMaxLines(Line.Info info) {
        if (info.getLineClass() == SourceDataLine.class)
            return NOT_SPECIFIED;
        if (info.getLineClass() == Clip.class)
            return NOT_SPECIFIED;
        return 0;
    }

    @Override
    public Info getMixerInfo() {
        return info;
    }

    @Override
    public Line.Info[] getSourceLineInfo() {
        return Arrays.copyOf(sourceLineInfo, sourceLineInfo.length);
    }

    @Override
    public Line.Info[] getSourceLineInfo(Line.Info info) {
        ArrayList<Line.Info> infoList = new ArrayList<>();

        for (Line.Info value : sourceLineInfo) {
            if (info.matches(value)) {
                infoList.add(value);
            }
        }
        return infoList.toArray(new Line.Info[0]);
    }

    @Override
    public Line[] getSourceLines() {
        synchronized (control_mutex) {
            return openLines.toArray(new Line[0]);
        }
    }

    @Override
    public Line.Info[] getTargetLineInfo() {
        return new Line.Info[0];
    }

    @Override
    public Line.Info[] getTargetLineInfo(Line.Info info) {
        return new Line.Info[0];
    }

    @Override
    public Line[] getTargetLines() {
        return new Line[0];
    }

    @Override
    public boolean isLineSupported(Line.Info info) {
        if (info != null) {
            for (Line.Info value : sourceLineInfo) {
                if (info.matches(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isSynchronizationSupported(Line[] lines, boolean maintainSync) {
        return false;
    }

    @Override
    public void synchronize(Line[] lines, boolean maintainSync) {
        throw new UnsupportedOperationException("Synchronization is not supported by this mixer.");
    }

    @Override
    public void unsynchronize(Line[] lines) {
        throw new UnsupportedOperationException("Synchronization is not supported by this mixer.");
    }

    @Override
    public void addLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.add(listener);
        }
    }

    private void sendEvent(LineEvent event) {
        listeners.forEach(listener -> listener.update(event));
    }

    @Override
    public void close() {
        if (!isOpen())
            return;
        sendEvent(new LineEvent(this, LineEvent.Type.CLOSE, NOT_SPECIFIED));

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

                try {
                    tempPusherStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        synchronized (control_mutex) {
            open = false;
            openLines.forEach(Line::close);
            if (sourceDataLine != null) {
                sourceDataLine.drain();
                sourceDataLine.close();
                sourceDataLine = null;
            }
        }
    }

    @Override
    public Control getControl(Type control) {
        throw new IllegalArgumentException("Unsupported control type : " + control);
    }

    @Override
    public Control[] getControls() {
        return new Control[0];
    }

    @Override
    public Line.Info getLineInfo() {
        return new Line.Info(Mixer.class);
    }

    @Override
    public boolean isControlSupported(Type control) {
        return false;
    }

    @Override
    public boolean isOpen() {
        synchronized (control_mutex) {
            return open;
        }
    }

    @Override
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
                    assert line != null : new IllegalArgumentException("No line matching " + info.toString() + " is supported.");
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
        LineEvent event;
        AudioInputStream inputStream;
        synchronized (control_mutex) {
            open = true;
            implicitOpen = false;
            if (targetFormat != null)
                format = targetFormat;
            event = new LineEvent(this, LineEvent.Type.OPEN, NOT_SPECIFIED);
            inputStream = getInputStream();
        }
        sendEvent(event);
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

        SoftChorus chorus = new SoftChorus(sampleRate, controlRate);
        chorus.setMixMode(true);
        chorus.setInput(0, buffers[CHANNEL_EFFECT2]);
        chorus.setOutput(0, buffers[CHANNEL_LEFT]);
        if (channels != 1) chorus.setOutput(1, buffers[CHANNEL_RIGHT]);
        chorus.setOutput(2, buffers[CHANNEL_EFFECT1]);

        SoftReverb reverb = new SoftReverb(sampleRate, controlRate);
        reverb.setMixMode(true);
        reverb.setInput(0, buffers[CHANNEL_EFFECT1]);
        reverb.setOutput(0, buffers[CHANNEL_LEFT]);
        if (channels != 1) reverb.setOutput(1, buffers[CHANNEL_RIGHT]);

        SoftLimiter limiter = new SoftLimiter(sampleRate, controlRate);
        limiter.setMixMode(false);
        limiter.setInput(0, buffers[CHANNEL_LEFT]);
        limiter.setOutput(0, buffers[CHANNEL_LEFT]);
        if (channels != 1) {
            limiter.setInput(1, buffers[CHANNEL_RIGHT]);
            limiter.setOutput(1, buffers[CHANNEL_RIGHT]);
        }
        List<SoftAudioProcessor> processors = Arrays.asList(chorus, reverb, limiter);
        InputStream in = new InputStream() {
            int bufferPos = 0;
            byte[] buffer = new byte[bufferSize * (format.getSampleSizeInBits() / 8) * channels];
            byte[] single = new byte[1];

            public void fillBuffer() {
                for (SoftAudioBuffer buffer : buffers) {
                    buffer.clear();
                }
                synchronized (control_mutex) {
                    openLines.forEach(SoftMixingDataLine::processControlLogic);
                    processors.forEach(SoftAudioProcessor::processControlLogic);
                }
                openLines.forEach(openLine -> openLine.processAudioLogic(buffers));
                processors.forEach(SoftAudioProcessor::processAudio);
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

    public void openLine(SoftMixingDataLine line) {
        synchronized (control_mutex) {
            openLines.add(line);
        }
    }

    public void closeLine(SoftMixingDataLine line) {
        synchronized (control_mutex) {
            openLines.remove(line);
        }
    }

    @Override
    public void removeLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.remove(listener);
        }
    }

    public long getLatency() {
        synchronized (control_mutex) {
            return latency;
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
