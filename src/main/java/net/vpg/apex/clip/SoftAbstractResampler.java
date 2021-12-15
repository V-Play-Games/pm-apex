/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract resampler class.
 *
 * @author Karl Helgason
 */
public abstract class SoftAbstractResampler implements SoftResampler {
    @Override
    public final SoftResamplerStream openStream() {
        return new ModelAbstractResamplerStream();
    }

    private class ModelAbstractResamplerStream implements SoftResamplerStream {
        static final int SECTOR_SIZE = 400;
        final AtomicFloat current_pitch = new AtomicFloat();
        final AtomicFloat ix = new AtomicFloat();
        final AtomicInteger ox = new AtomicInteger();
        final int pad;
        final int pad2;
        AudioFloatInputStream stream;
        boolean stream_eof = false;
        int loopMode;
        boolean loopDirection = true; // true = forward
        int loopStart;
        int loopLength;
        float target_pitch;
        boolean started;
        boolean eof;
        int sector_pos = 0;
        int sector_loop_start = -1;
        boolean markSet = false;
        int markLimit = 0;
        int streamPos = 0;
        int channels = 2;
        float[][] buffer;
        boolean bufferOrder;
        float[] sBuffer;
        float sampleRateConverter = 1.0F;
        float pitchCorrection = 0.0F;

        ModelAbstractResamplerStream() {
            pad = getPadding();
            pad2 = getPadding() * 2;
            buffer = new float[2][SECTOR_SIZE + pad2];
            bufferOrder = true;
        }

        @Override
        public void open(ModelWavetable osc, float outputRate) throws IOException {
            eof = false;
            channels = osc.getChannels();
            if (buffer.length < channels) {
                buffer = new float[channels][SECTOR_SIZE + pad2];
            }

            stream = osc.openStream();
            streamPos = 0;
            stream_eof = false;
            pitchCorrection = osc.getPitchCorrection();
            sampleRateConverter = stream.getFormat().getSampleRate() / outputRate;
            loopLength = osc.getLoopLength();
            loopStart = osc.getLoopStart();
            sector_loop_start = loopStart / SECTOR_SIZE - 1;

            sector_pos = 0;

            if (sector_loop_start < 0) {
                sector_loop_start = 0;
            }
            started = false;
            loopMode = osc.getLoopType();

            if (loopMode != 0) {
                markSet = false;
                markLimit = channels * (loopLength + pad2 + 1);
            } else {
                markSet = true;
            }

            target_pitch = sampleRateConverter;
            current_pitch.set(sampleRateConverter);

            bufferOrder = true;
            loopDirection = true;

            for (int i = 0; i < channels; i++) {
                Arrays.fill(buffer[i], SECTOR_SIZE, SECTOR_SIZE + pad2, 0);
            }
            eof = false;

            ix.set(SECTOR_SIZE + pad);
            sector_pos = -1;
            streamPos = -SECTOR_SIZE;

            nextBuffer();
        }

        public void nextBuffer() throws IOException {
            if (ix.get() < pad) {
                if (markSet) {
                    // reset to target sector
                    stream.reset();
                    ix.addAndGet(streamPos - sector_loop_start * SECTOR_SIZE);
                    sector_pos = sector_loop_start;
                    streamPos = sector_pos * SECTOR_SIZE;

                    // go one sector backward
                    ix.addAndGet(SECTOR_SIZE);
                    sector_pos -= 1;
                    streamPos -= SECTOR_SIZE;
                    stream_eof = false;
                }
            }

            if (ix.get() >= SECTOR_SIZE + pad) {
                if (stream_eof) {
                    eof = true;
                    return;
                }
            }

            if (ix.get() >= SECTOR_SIZE * 4 + pad) {
                int skips = (int) ((ix.get() - SECTOR_SIZE * 4 + pad) / SECTOR_SIZE);
                ix.addAndGet(-SECTOR_SIZE * skips);
                sector_pos += skips;
                streamPos += SECTOR_SIZE * skips;
                stream.skip(SECTOR_SIZE * skips);
            }

            while (ix.get() >= SECTOR_SIZE + pad) {
                if (!markSet) {
                    if (sector_pos + 1 == sector_loop_start) {
                        stream.mark(markLimit);
                        markSet = true;
                    }
                }
                ix.addAndGet(-SECTOR_SIZE);
                sector_pos++;
                streamPos += SECTOR_SIZE;

                for (int c = 0; c < channels; c++) {
                    float[] cBuffer = buffer[c];
                    if (pad2 >= 0) System.arraycopy(cBuffer, SECTOR_SIZE, cBuffer, 0, pad2);
                }

                int read;
                if (channels == 1) {
                    read = stream.read(buffer[0], pad2, SECTOR_SIZE);
                } else {
                    int len = SECTOR_SIZE * channels;
                    if (sBuffer == null || sBuffer.length < len)
                        sBuffer = new float[len];
                    read = stream.read(sBuffer, 0, len);
                    if (read != -1) {
                        read = read / channels;
                        for (int i = 0; i < channels; i++) {
                            float[] cBuffer = buffer[i];
                            int ix = i;
                            int ox = pad2;
                            for (int j = 0; j < read; j++, ix += channels, ox++) {
                                cBuffer[ox] = sBuffer[ix];
                            }
                        }
                    }
                }

                if (read == -1) {
                    stream_eof = true;
                    for (int i = 0; i < channels; i++) {
                        Arrays.fill(buffer[i], pad2, pad2 + SECTOR_SIZE, 0f);
                    }
                    return;
                }
                if (read != SECTOR_SIZE) {
                    for (int i = 0; i < channels; i++) {
                        Arrays.fill(buffer[i], pad2 + read, pad2 + SECTOR_SIZE, 0f);
                    }
                }

                bufferOrder = true;
            }
        }

        public void reverseBuffers() {
            bufferOrder = !bufferOrder;
            for (int c = 0; c < channels; c++) {
                float[] cBuffer = buffer[c];
                int len = cBuffer.length - 1;
                int len2 = cBuffer.length / 2;
                for (int i = 0; i < len2; i++) {
                    float x = cBuffer[i];
                    cBuffer[i] = cBuffer[len - i];
                    cBuffer[len - i] = x;
                }
            }
        }

        @Override
        public int read(float[][] buffer, int offset, int len) throws IOException {
            if (eof)
                return -1;

            float pitchStep = (target_pitch - current_pitch.get()) / len;
            started = true;

            ox.set(offset);
            int ox_end = len + offset;

            float ix_end = SECTOR_SIZE + pad;
            if (!loopDirection) {
                ix_end = pad;
            }
            while (ox.get() != ox_end) {
                nextBuffer();
                if (!loopDirection) {
                    // If we are in backward playing part of ping pong, then reverse loop
                    if (streamPos < loopStart + pad) {
                        ix_end = loopStart - streamPos + pad2;
                        if (ix.get() <= ix_end) {
                            if ((loopMode & 4) != 0) {
                                // ping pong loop, change loop direction
                                loopDirection = true;
                                ix_end = SECTOR_SIZE + pad;
                                continue;
                            }

                            ix.addAndGet(loopLength);
                            ix_end = pad;
                            continue;
                        }
                    }

                    if (bufferOrder)
                        reverseBuffers();

                    ix.set(SECTOR_SIZE + pad2 - ix.get());
                    ix_end = SECTOR_SIZE + pad2 - ix_end;
                    ix_end++;

                    float bak_ix = ix.get();
                    int bak_ox = ox.get();
                    float bak_pitch = current_pitch.get();
                    for (int i = 0; i < channels; i++) {
                        float[] cBuffer = buffer[i];
                        if (cBuffer != null) {
                            ix.set(bak_ix);
                            ox.set(bak_ox);
                            current_pitch.set(bak_pitch);
                            interpolate(buffer[i], ix, ix_end, current_pitch, pitchStep, cBuffer, ox, ox_end);
                        }
                    }

                    ix.set(SECTOR_SIZE + pad2 - ix.get());
                    ix_end--;
                    ix_end = (SECTOR_SIZE + pad2) - ix_end;

                    if (eof) {
                        current_pitch.set(target_pitch);
                        return ox.get() - offset;
                    }
                    continue;
                }
                if (loopMode != 0) {
                    if (streamPos + SECTOR_SIZE > loopLength + loopStart + pad) {
                        ix_end = loopStart + loopLength - streamPos + pad2;
                        if (ix.get() >= ix_end) {
                            if ((loopMode & 4) != 0 || (loopMode & 8) != 0) {
                                // ping pong or reverse loop, change loop direction
                                loopDirection = false;
                                ix_end = pad;
                                continue;
                            }
                            ix_end = SECTOR_SIZE + pad;
                            ix.addAndGet(-loopLength);
                            continue;
                        }
                    }
                }

                if (!bufferOrder) {
                    reverseBuffers();
                }

                float bak_ix = ix.get();
                int bak_ox = ox.get();
                float bak_pitch = current_pitch.get();
                for (int i = 0; i < channels; i++) {
                    float[] cBuffer = buffer[i];
                    if (cBuffer != null) {
                        ix.set(bak_ix);
                        ox.set(bak_ox);
                        current_pitch.set(bak_pitch);
                        interpolate(buffer[i], ix, ix_end, current_pitch, pitchStep, cBuffer, ox, ox_end);
                    }
                }

                if (eof) {
                    current_pitch.set(target_pitch);
                    return ox.get() - offset;
                }
            }

            current_pitch.set(target_pitch);
            return len;
        }

    }
}
