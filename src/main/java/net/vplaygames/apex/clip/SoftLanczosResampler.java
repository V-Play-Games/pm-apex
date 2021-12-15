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
package net.vplaygames.apex.clip;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lanczos interpolation resampler.
 *
 * @author Karl Helgason
 */
public final class SoftLanczosResampler extends SoftAbstractResampler {
    private static final int sincTablesCount = 2000;
    private static final int sincTableSize = 5;
    private static final int sincTableCenter = sincTableSize / 2;
    private static float[][] sincTables;

    public SoftLanczosResampler() {
        ensureTablesSet();
    }

    private static void ensureTablesSet() {
        if (sincTables == null) {
            synchronized (sincTables = new float[sincTablesCount][]) {
                for (int i = 0; i < sincTablesCount; i++) {
                    float offset = i / (float) sincTablesCount;
                    float[] w = sincTables[i] = new float[sincTableSize];
                    for (int k = 0; k < sincTableSize; k++) {
                        float x = k - sincTableCenter - offset;
                        if (x < -2 || x > 2) {
                            w[k] = 0;
                        } else if (x == 0) {
                            w[k] = 1;
                        } else {
                            double pix = Math.PI * x;
                            w[k] = (float) (2.0 * Math.sin(pix) * Math.sin(pix / 2.0) / (pix * pix));
                        }
                    }
                }
            }
        }
    }

    @Override
    public int getPadding() { // must be at least half of sinc_table_size
        return sincTableCenter + 2;
    }

    @Override
    public void interpolate(float[] in, AtomicFloat in_offset, float in_end,
                            AtomicFloat pitch, float pitch_step,
                            float[] out, AtomicInteger out_offset, int out_end) {
        while (in_offset.get() < in_end && out_offset.get() < out_end) {
            int iix = in_offset.intValue();
            float[] sincTable = sincTables[(int) ((in_offset.get() - iix) * sincTablesCount)];
            int i = 0;
            int xx = iix - sincTableCenter;
            float y = 0;
            while (i < sincTableSize) {
                y += in[xx++] * sincTable[i++];
            }
            out[out_offset.getAndIncrement()] = y;
            if (pitch_step != 0) {
                in_offset.addAndGet(pitch.get());
                pitch.addAndGet(pitch_step);
            }
        }
    }
}
