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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * General software mixing line.
 *
 * @author Karl Helgason
 */
public abstract class SoftMixingDataLine implements DataLine {
    public static final FloatControl.Type CHORUS_SEND = new FloatControl.Type("Chorus Send") {
    };
    protected final Object control_mutex;
    protected final Gain gain = new Gain();
    protected final Mute mute = new Mute();
    protected final Balance balance = new Balance();
    protected final Pan pan = new Pan();
    protected final ReverbSend reverbSend = new ReverbSend();
    protected final ChorusSend chorusSend = new ChorusSend();
    protected final ApplyReverb applyReverb = new ApplyReverb();
    protected final Control[] controls;
    protected int frameSize;
    protected int bufferSize;
    protected float[] readBuffer;
    protected boolean open = false;
    protected float leftGain = 1;
    protected float rightGain = 1;
    protected float eff1Gain = 0;
    protected float eff2Gain = 0;
    protected List<LineListener> listeners = new ArrayList<>();
    protected SoftMixingMixer mixer;
    protected Info info;
    protected boolean stereo;
    protected AudioFloatInputStream inputStream;
    protected int out_channels;
    protected int in_channels;
    protected boolean active = false;

    SoftMixingDataLine(SoftMixingMixer mixer, Info info) {
        this.mixer = mixer;
        this.info = info;
        this.control_mutex = mixer.control_mutex;
        this.controls = new Control[]{gain, mute, balance, pan, reverbSend, chorusSend, applyReverb};
        calcVolume();
    }

    protected abstract void processControlLogic();

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
            fillReadData(buffers[SoftMixingMixer.CHANNEL_LEFT].getArray(), 0, leftGain);
            if (out_channels != 1)
                fillReadData(buffers[SoftMixingMixer.CHANNEL_RIGHT].getArray(), in_channels - 1, rightGain);
            if (eff1Gain > 0.0002) {
                float[] eff = buffers[SoftMixingMixer.CHANNEL_EFFECT1].getArray();
                fillReadData(eff, 0, eff1Gain);
                if (stereo)
                    fillReadData(eff, 1, eff1Gain);
            }
            if (eff1Gain > 0.0002) {
                float[] eff = buffers[SoftMixingMixer.CHANNEL_EFFECT2].getArray();
                fillReadData(eff, 0, eff2Gain);
                if (stereo)
                    fillReadData(eff, 1, eff2Gain);
            }
        }
    }

    private void fillReadData(float[] dest, int ix, float factor) {
        for (int i = 0, len = dest.length; i < len; ix += in_channels) {
            dest[i++] += readBuffer[ix] * factor;
        }
    }

    final void calcVolume() {
        synchronized (control_mutex) {
            double gainValue = Math.pow(10.0, gain.getValue() / 20.0);
            if (mute.getValue())
                gainValue = 0;
            leftGain = (float) gainValue;
            rightGain = (float) gainValue;
            if (mixer.getFormat().getChannels() > 1) {
                // -1 = Left, 0 = Center, 1 = Right
                double balanceValue = balance.getValue();
                if (balanceValue > 0)
                    leftGain *= 1 - balanceValue;
                else
                    rightGain *= 1 + balanceValue;

            }
        }

        eff1Gain = (float) Math.pow(10.0, reverbSend.getValue() / 20.0);
        eff2Gain = (float) Math.pow(10.0, chorusSend.getValue() / 20.0);

        if (!applyReverb.getValue()) {
            eff1Gain = 0;
        }
    }

    final void sendEvent(LineEvent event) {
        listeners.forEach(listener -> listener.update(event));
    }

    @Override
    public final void addLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.add(listener);
        }
    }

    @Override
    public final void removeLineListener(LineListener listener) {
        synchronized (control_mutex) {
            listeners.remove(listener);
        }
    }

    @Override
    public final Info getLineInfo() {
        return info;
    }

    @Override
    public final Control getControl(Type control) {
        if (control != null) {
            for (Control value : controls) {
                if (value.getType() == control) {
                    return value;
                }
            }
        }
        throw new IllegalArgumentException("Unsupported control type : " + control);
    }

    @Override
    public final Control[] getControls() {
        return Arrays.copyOf(controls, controls.length);
    }

    @Override
    public final boolean isControlSupported(Type control) {
        if (control != null) {
            for (Control value : controls) {
                if (value.getType() == control) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static final class AudioFloatInputStreamResampler extends AudioFloatInputStream {
        private final AudioFloatInputStream ais;
        private final AudioFormat targetFormat;
        private final AtomicFloat pitch;
        private final float[] ibuffer2;
        private final float[][] ibuffer;
        private final int pad;
        private final int pad2;
        private final AtomicFloat ix = new AtomicFloat();
        private final AtomicInteger ox = new AtomicInteger();
        private final SoftResampler resampler;
        private final int nrofchannels;
        private float[] skipbuffer;
        private float ibuffer_index;
        private int ibuffer_len;
        private float[][] cbuffer;
        private float[][] mark_ibuffer = null;
        private float mark_ibuffer_index = 0;
        private int mark_ibuffer_len = 0;

        public AudioFloatInputStreamResampler(AudioFloatInputStream ais, AudioFormat format) {
            this.ais = ais;
            AudioFormat sourceFormat = ais.getFormat();
            targetFormat = new AudioFormat(sourceFormat.getEncoding(),
                format.getSampleRate(),
                sourceFormat.getSampleSizeInBits(),
                sourceFormat.getChannels(),
                sourceFormat.getFrameSize(),
                format.getSampleRate(),
                sourceFormat.isBigEndian());
            nrofchannels = targetFormat.getChannels();
            switch (String.valueOf(format.getProperty("interpolation"))) {
                case "point":
                    this.resampler = new SoftPointResampler();
                    break;
                case "linear":
                    this.resampler = new SoftLinearResampler();
                    break;
                case "cubic":
                    this.resampler = new SoftCubicResampler();
                    break;
                case "lanczos":
                    this.resampler = new SoftLanczosResampler();
                    break;
                case "sinc":
                    this.resampler = new SoftSincResampler();
                    break;
                default:
                    this.resampler = new SoftLinearResampler2();
                    break;
            }
            pitch = new AtomicFloat(sourceFormat.getSampleRate() / format.getSampleRate());
            pad = resampler.getPadding();
            pad2 = pad * 2;
            int buffer_len = 512;
            ibuffer = new float[nrofchannels][buffer_len + pad2];
            ibuffer2 = new float[nrofchannels * buffer_len];
            ibuffer_index = buffer_len + pad;
            ibuffer_len = buffer_len;
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void close() throws IOException {
            ais.close();
        }

        @Override
        public AudioFormat getFormat() {
            return targetFormat;
        }

        @Override
        public long getFrameLength() {
            return AudioSystem.NOT_SPECIFIED; // ais.getFrameLength();
        }

        @Override
        public void mark(int limit) {
            ais.mark((int) (limit * pitch.get()));
            mark_ibuffer_index = ibuffer_index;
            mark_ibuffer_len = ibuffer_len;
            if (mark_ibuffer == null) {
                mark_ibuffer = new float[ibuffer.length][ibuffer[0].length];
            }
            for (int c = 0; c < ibuffer.length; c++) {
                float[] from = ibuffer[c];
                float[] to = mark_ibuffer[c];
                System.arraycopy(from, 0, to, 0, to.length);
            }
        }

        @Override
        public boolean markSupported() {
            return ais.markSupported();
        }

        private void readNextBuffer() throws IOException {
            if (ibuffer_len == -1)
                return;

            for (int c = 0; c < nrofchannels; c++) {
                float[] buff = ibuffer[c];
                int buffer_len_pad = ibuffer_len + pad2;
                for (int i = ibuffer_len, ix = 0; i < buffer_len_pad; i++, ix++) {
                    buff[ix] = buff[i];
                }
            }

            ibuffer_index -= ibuffer_len;

            ibuffer_len = ais.read(ibuffer2);
            if (ibuffer_len >= 0) {
                while (ibuffer_len < ibuffer2.length) {
                    int ret = ais.read(ibuffer2, ibuffer_len, ibuffer2.length
                        - ibuffer_len);
                    if (ret == -1)
                        break;
                    ibuffer_len += ret;
                }
                Arrays.fill(ibuffer2, ibuffer_len, ibuffer2.length, 0);
                ibuffer_len /= nrofchannels;
            } else {
                Arrays.fill(ibuffer2, 0, ibuffer2.length, 0);
            }

            int ibuffer2_len = ibuffer2.length;
            for (int c = 0; c < nrofchannels; c++) {
                float[] buff = ibuffer[c];
                for (int i = c, ix = pad2; i < ibuffer2_len; i += nrofchannels, ix++) {
                    buff[ix] = ibuffer2[i];
                }
            }
        }

        @Override
        public int read(float[] b, int off, int len) throws IOException {
            if (cbuffer == null || cbuffer[0].length < len / nrofchannels) {
                cbuffer = new float[nrofchannels][len / nrofchannels];
            }
            if (ibuffer_len == -1)
                return -1;
            if (len < 0)
                return 0;
            int remain = len / nrofchannels;
            int destPos = 0;
            int in_end = ibuffer_len;
            while (remain > 0) {
                if (ibuffer_len >= 0) {
                    if (ibuffer_index >= ibuffer_len + pad)
                        readNextBuffer();
                    in_end = ibuffer_len + pad;
                }

                if (ibuffer_len < 0) {
                    in_end = pad2;
                    if (ibuffer_index >= in_end)
                        break;
                }

                if (ibuffer_index < 0)
                    break;
                int preDestPos = destPos;
                for (int c = 0; c < nrofchannels; c++) {
                    ix.set(ibuffer_index);
                    ox.set(destPos);
                    float[] buff = ibuffer[c];
                    resampler.interpolate(buff, ix, in_end, pitch, 0, cbuffer[c], ox, len / nrofchannels);
                }
                ibuffer_index = ix.get();
                destPos = ox.get();
                remain -= destPos - preDestPos;
            }
            for (int c = 0; c < nrofchannels; c++) {
                int ix = 0;
                float[] buff = cbuffer[c];
                for (int i = c; i < b.length; i += nrofchannels) {
                    b[i] = buff[ix++];
                }
            }
            return len - remain * nrofchannels;
        }

        @Override
        public void reset() throws IOException {
            ais.reset();
            if (mark_ibuffer == null)
                return;
            ibuffer_index = mark_ibuffer_index;
            ibuffer_len = mark_ibuffer_len;
            for (int c = 0; c < ibuffer.length; c++) {
                float[] from = mark_ibuffer[c];
                float[] to = ibuffer[c];
                System.arraycopy(from, 0, to, 0, to.length);
            }
        }

        @Override
        public long skip(long len) throws IOException {
            if (len < 0)
                return 0;
            if (skipbuffer == null)
                skipbuffer = new float[1024 * targetFormat.getFrameSize()];
            long remain = len;
            while (remain > 0) {
                int ret = read(skipbuffer, 0, (int) Math.min(remain,
                    skipbuffer.length));
                if (ret < 0) {
                    if (remain == len)
                        return ret;
                    break;
                }
                remain -= ret;
            }
            return len - remain;
        }
    }

    private final class Gain extends FloatControl {
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

    private final class Mute extends BooleanControl {
        private Mute() {
            super(BooleanControl.Type.MUTE, false, "True", "False");
        }

        @Override
        public void setValue(boolean newValue) {
            super.setValue(newValue);
            calcVolume();
        }
    }

    private final class ApplyReverb extends BooleanControl {
        private ApplyReverb() {
            super(BooleanControl.Type.APPLY_REVERB, false, "True", "False");
        }

        @Override
        public void setValue(boolean newValue) {
            super.setValue(newValue);
            calcVolume();
        }
    }

    private final class Balance extends FloatControl {
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

    private final class Pan extends FloatControl {
        private Pan() {
            super(FloatControl.Type.PAN, -1.0f, 1.0f, 1.0f / 128.0f, -1,
                0.0f, "", "Left", "Center", "Right");
        }

        @Override
        public float getValue() {
            return balance.getValue();
        }

        @Override
        public void setValue(float newValue) {
            super.setValue(newValue);
            balance.setValue(newValue);
        }
    }

    private final class ReverbSend extends FloatControl {
        private ReverbSend() {
            super(FloatControl.Type.REVERB_SEND, -80f, 6.0206f, 80f / 128.0f,
                -1, -80f, "dB", "Minimum", "", "Maximum");
        }

        @Override
        public void setValue(float newValue) {
            super.setValue(newValue);
            balance.setValue(newValue);
        }
    }

    private final class ChorusSend extends FloatControl {
        private ChorusSend() {
            super(CHORUS_SEND, -80f, 6.0206f, 80f / 128.0f, -1, -80f, "dB",
                "Minimum", "", "Maximum");
        }

        @Override
        public void setValue(float newValue) {
            super.setValue(newValue);
            balance.setValue(newValue);
        }
    }
}
