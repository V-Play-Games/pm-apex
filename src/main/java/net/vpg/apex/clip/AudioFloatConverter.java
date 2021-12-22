/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

/**
 * This class is used to convert between audio byte buffers and audio float buffers.
 *
 * @author Karl Helgason
 */
public abstract class AudioFloatConverter {
    private static Map<Integer, List<AudioFloatConverter>> cachedConverters = new HashMap<>();
    protected final int bits;
    protected final boolean signed;
    protected final boolean bigEndian;

    protected AudioFloatConverter(AudioFloatConverter converter) {
        this(converter.bits, converter.signed, converter.bigEndian);
    }

    protected AudioFloatConverter(AudioFormat format) {
        this(format.getSampleSizeInBits(), format.getEncoding().equals(Encoding.PCM_SIGNED), format.isBigEndian());
    }

    protected AudioFloatConverter(int bits, boolean signed, boolean bigEndian) {
        this.bits = bits;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public static AudioFloatConverter getConverter(AudioFormat format) {
        int sampleSize = format.getSampleSizeInBits();
        int mode = (sampleSize + 7) / 8;
        if (format.getFrameSize() != mode * format.getChannels()) {
            return null;
        }
        List<AudioFloatConverter> converters = cachedConverters.get(sampleSize);
        if (converters != null) {
            AudioFloatConverter cached = converters.stream().filter(c -> c.canConvert(format)).findFirst().orElse(null);
            if (cached != null) {
                return cached;
            }
        }
        if (!format.getEncoding().equals(Encoding.PCM_FLOAT)) {
            AudioFloatConverter converter;
            switch (mode) {
                case 1:
                    converter = new PcmInt8(format);
                    break;
                case 2:
                    converter = new PcmInt16(format);
                    break;
                case 3:
                    converter = new PcmInt24(format);
                    break;
                case 4:
                    converter = new PcmInt32(format);
                    break;
                default:
                    converter = new PcmInt32x(format, mode - 4);
                    break;
            }
            if (sampleSize % 8 == 0)
                return cache(converter);
            else
                return cache(new AudioFloatLSBFilter(converter));
        } else if (sampleSize == 32 || sampleSize == 64) {
            return cache(new PcmFloat(format));
        }
        return null;
    }

    private static AudioFloatConverter cache(AudioFloatConverter converter) {
        cachedConverters.computeIfAbsent(converter.bits, x -> new ArrayList<>()).add(converter);
        return converter;
    }

    public static boolean isSupported(AudioFormat format) {
        int sampleSize = format.getSampleSizeInBits();
        // frame size should be valid
        // AND
        // should either be PCM_INT, or the sampleSize should be either 32 or 64
        return format.getFrameSize() == (sampleSize + 7) / 8 * format.getChannels()
            && (!format.getEncoding().equals(Encoding.PCM_FLOAT) || (sampleSize == 32 || sampleSize == 64));
    }

    public int getBits() {
        return bits;
    }

    public boolean isSigned() {
        return signed;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public boolean canConvert(AudioFormat format) {
        return format.getSampleSizeInBits() == bits && signed == format.getEncoding().equals(Encoding.PCM_SIGNED) && bigEndian == format.isBigEndian();
    }

    public abstract float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len);

    public final float[] toFloatArray(byte[] in, float[] out, int outOffset, int len) {
        return toFloatArray(in, 0, out, outOffset, len);
    }

    public final float[] toFloatArray(byte[] in, int inOffset, float[] out, int len) {
        return toFloatArray(in, inOffset, out, 0, len);
    }

    public final float[] toFloatArray(byte[] in, float[] out, int len) {
        return toFloatArray(in, 0, out, 0, len);
    }

    public final float[] toFloatArray(byte[] in, float[] out) {
        return toFloatArray(in, 0, out, 0, out.length);
    }

    public abstract byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset);

    public final byte[] toByteArray(float[] in, int len, byte[] out, int outOffset) {
        return toByteArray(in, 0, len, out, outOffset);
    }

    public final byte[] toByteArray(float[] in, int inOffset, int len, byte[] out) {
        return toByteArray(in, inOffset, len, out, 0);
    }

    public final byte[] toByteArray(float[] in, int len, byte[] out) {
        return toByteArray(in, 0, len, out, 0);
    }

    public final byte[] toByteArray(float[] in, byte[] out) {
        return toByteArray(in, 0, in.length, out, 0);
    }

    /**
     * LSB Filter, used filter least significant byte in samples arrays.
     * <p>
     * Is used filter out data in lsb byte when SampleSizeInBits is not
     * divisible by 8.
     */
    private static class AudioFloatLSBFilter extends AudioFloatConverter {
        private final AudioFloatConverter converter;
        private final int offset;
        private final int stepSize;
        private final byte mask;

        private AudioFloatLSBFilter(AudioFloatConverter converter) {
            super(converter);
            this.converter = converter;
            stepSize = bits / 8 + 1;
            offset = bigEndian ? stepSize - 1 : 0;
            mask = getMask(bits);
        }

        private static byte getMask(int sampleSize) {
            switch (sampleSize % 8) {
                case 0:
                    return (byte) 0x00;
                case 1:
                    return (byte) 0x80;
                case 2:
                    return (byte) 0xC0;
                case 3:
                    return (byte) 0xE0;
                case 4:
                    return (byte) 0xF0;
                case 5:
                    return (byte) 0xF8;
                case 6:
                    return (byte) 0xFC;
                case 7:
                    return (byte) 0xFE;
                default:
                    return (byte) 0xFF;
            }
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            byte[] buffer = converter.toByteArray(in, inOffset, len, out, outOffset);
            for (int i = outOffset + offset, outOffset_end = len * stepSize; i < outOffset_end; i += stepSize) {
                out[i] = (byte) (out[i] & mask);
            }
            return buffer;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len) {
            byte[] masked_buffer = Arrays.copyOf(in, in.length);
            for (int i = inOffset + offset, inOffset_end = len * stepSize; i < inOffset_end; i += stepSize) {
                masked_buffer[i] = (byte) (masked_buffer[i] & mask);
            }
            return converter.toFloatArray(masked_buffer, inOffset, out, outOffset, len);
        }

    }

    // PCM 32-bit/64-bit float
    private static class PcmFloat extends AudioFloatConverter {
        private final ByteOrder order;
        private final int factor;

        private PcmFloat(AudioFormat format) {
            super(format);
            this.order = bigEndian ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN;
            this.factor = bits == 32 ? 4 : 8;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int length) {
            int len = length * factor;
            ByteBuffer bytebuffer = ByteBuffer.allocate(len).order(order);
            bytebuffer.put(in, inOffset, len);
            bytebuffer.asFloatBuffer().get(out, outOffset, length);
            return out;
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            int length = len * factor;
            ByteBuffer bytebuffer = ByteBuffer.allocate(length).order(order);
            bytebuffer.asFloatBuffer().put(in, inOffset, len);
            bytebuffer.get(out, outOffset, length);
            return out;
        }
    }

    // PCM 8-bit
    private static class PcmInt8 extends AudioFloatConverter {
        private final int difference;

        public PcmInt8(AudioFormat format) {
            super(format);
            this.difference = signed ? 0x80 : 0;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len) {
            for (int i = outOffset, fence = outOffset + len; i < fence; i++) {
                int x = in[inOffset++] - difference;
                out[i] = x > 0 ? x / 127.0f : x / 128.0f;
            }
            return out;
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            for (int i = inOffset, fence = inOffset + len; i < fence; i++) {
                float x = in[i];
                out[outOffset++] = (byte) (difference + (x > 0 ? x * 127 : x * 128));
            }
            return out;
        }

        @Override
        public boolean canConvert(AudioFormat format) {
            return format.getSampleSizeInBits() == bits && signed == format.getEncoding().equals(Encoding.PCM_SIGNED);
        }
    }

    // PCM 16-bit
    private static class PcmInt16 extends AudioFloatConverter {
        private final int difference;
        private final int shift1;
        private final int shift2;

        private PcmInt16(AudioFormat format) {
            super(format);
            this.difference = signed ? 0x8000 : 0;
            this.shift1 = bigEndian ? 8 : 0;
            this.shift2 = bigEndian ? 0 : 8;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len) {
            for (int i = outOffset, fence = outOffset + len; i < fence; i++) {
                int x = ((in[inOffset++] & 0xFF) << shift1 | (in[inOffset++] & 0xFF) << shift2) - difference;
                out[i] = x > 0 ? x / 32767.0f : x / 32768.0f;
            }
            return out;
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            for (int i = inOffset, fence = inOffset + len; i < fence; i++) {
                float f = in[i];
                int x = (int) (f > 0 ? f * 32767 : f * 32768) + difference;
                out[outOffset++] = (byte) (x >>> shift1);
                out[outOffset++] = (byte) (x >>> shift2);
            }
            return out;
        }
    }

    // PCM 24-bit
    private static class PcmInt24 extends AudioFloatConverter {
        private final int shift1;
        private final int shift2;
        private final int shift3;

        private PcmInt24(AudioFormat format) {
            super(format);
            this.shift1 = bigEndian ? 16 : 0;
            this.shift2 = 8;
            this.shift3 = bigEndian ? 0 : 16;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len) {
            for (int i = outOffset, fence = outOffset + len; i < fence; i++) {
                int x = (in[inOffset++] & 0xFF) << shift1 |
                    (in[inOffset++] & 0xFF) << shift2 |
                    (in[inOffset++] & 0xFF) << shift3;
                x -= signed ? 0x800000 : x > 0x7FFFFF ? 0x1000000 : 0;
                out[i] = x > 0 ? x / 8388607.0f : x / 8388608.0f;
            }
            return out;
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            for (int i = inOffset, fence = inOffset + len; i < fence; i++) {
                float f = in[i];
                int x = (int) (f > 0 ? f * 8388607.0f : f * 8388608.0f);
                x += signed ? 0x800000 : x < 0 ? 0x1000000 : 0;
                out[outOffset++] = (byte) (x >>> shift1);
                out[outOffset++] = (byte) (x >>> shift2);
                out[outOffset++] = (byte) (x >>> shift1);
            }
            return out;
        }
    }

    // PCM 32-bit
    private static class PcmInt32 extends AudioFloatConverter {
        private final int difference;
        private final int shift1;
        private final int shift2;
        private final int shift3;
        private final int shift4;

        private PcmInt32(AudioFormat format) {
            super(format);
            this.difference = signed ? 0x80000000 : 0;
            this.shift1 = bigEndian ? 24 : 0;
            this.shift2 = bigEndian ? 16 : 8;
            this.shift3 = bigEndian ? 8 : 16;
            this.shift4 = bigEndian ? 0 : 24;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len) {
            for (int i = outOffset, fence = outOffset + len; i < fence; i++) {
                int x = ((in[inOffset++] & 0xFF) << shift1 |
                    (in[inOffset++] & 0xFF) << shift2 |
                    (in[inOffset++] & 0xFF) << shift3 |
                    (in[inOffset++] & 0xFF) << shift4)
                    - difference;
                out[i] = x / (float) 0x7FFFFFFF;
            }
            return out;
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            for (int i = inOffset, fence = inOffset + len; i < fence; i++) {
                int x = (int) (in[i] * 0x7FFFFFFF) + difference;
                out[outOffset++] = (byte) (x >>> shift1);
                out[outOffset++] = (byte) (x >>> shift2);
                out[outOffset++] = (byte) (x >>> shift3);
                out[outOffset++] = (byte) (x >>> shift4);
            }
            return out;
        }
    }

    // PCM 32+ bit
    private static class PcmInt32x extends AudioFloatConverter {
        private final int xBytes;
        private final int difference;
        private final int shift1;
        private final int shift2;
        private final int shift3;
        private final int shift4;

        private PcmInt32x(AudioFormat format, int xBytes) {
            super(format);
            this.xBytes = xBytes;
            this.difference = signed ? 0x80000000 : 0;
            this.shift1 = bigEndian ? 24 : 0;
            this.shift2 = bigEndian ? 16 : 8;
            this.shift3 = bigEndian ? 8 : 16;
            this.shift4 = bigEndian ? 0 : 24;
        }

        @Override
        public float[] toFloatArray(byte[] in, int inOffset, float[] out, int outOffset, int len) {
            for (int i = outOffset, fence = outOffset + len; i < fence; i++) {
                int x = ((in[inOffset++] & 0xFF) << shift1 |
                    (in[inOffset++] & 0xFF) << shift2 |
                    (in[inOffset++] & 0xFF) << shift3 |
                    (in[inOffset++] & 0xFF) << shift4)
                    - difference;
                inOffset += xBytes;
                out[i] = x / 2147483647.0f;
            }
            return out;
        }

        @Override
        public byte[] toByteArray(float[] in, int inOffset, int len, byte[] out, int outOffset) {
            for (int i = inOffset, fence = inOffset + len; i < fence; i++) {
                int x = (int) (in[i] * 2147483647.0f) + difference;
                if (!bigEndian) for (int j = 0; j < xBytes; j++) out[outOffset++] = 0;
                out[outOffset++] = (byte) (x >>> shift1);
                out[outOffset++] = (byte) (x >>> shift2);
                out[outOffset++] = (byte) (x >>> shift3);
                out[outOffset++] = (byte) (x >>> shift4);
                if (bigEndian) for (int j = 0; j < xBytes; j++) out[outOffset++] = 0;
            }
            return out;
        }
    }
}
