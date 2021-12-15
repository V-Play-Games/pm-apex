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
 * Hann windowed sinc interpolation resampler with anti-alias filtering.
 * <p>
 * Using 30 points for the interpolation.
 *
 * @author Karl Helgason
 */
public class SoftSincResampler extends SoftAbstractResampler {
    static final int sinc_scale_size = 100;
    static final int sinc_table_fsize = 800;
    static final int sinc_table_size = 30;
    static final int sinc_table_center = sinc_table_size / 2;
    float[][][] sinc_table;

    public SoftSincResampler() {
        super();
        sinc_table = new float[sinc_scale_size][sinc_table_fsize][];
        for (int s = 0; s < sinc_scale_size; s++) {
            float scale = (float) (1.0 / (1.0 + Math.pow(s, 1.1) / 10.0));
            for (int i = 0; i < sinc_table_fsize; i++) {
                sinc_table[s][i] = sincTable(-i / (float) sinc_table_fsize, scale);
            }
        }
    }

    // Generate sinc table
    private static float[] sincTable(float offset, float scale) {
        int center = sinc_table_size / 2;
        float[] w = wHanning(offset);
        for (int k = 0; k < sinc_table_size; k++)
            w[k] *= sinc((-center + k + offset) * scale) * scale;
        return w;
    }

    // Generate hann window suitable for windowing sinc
    private static float[] wHanning(float offset) {
        float[] window_table = new float[sinc_table_size];
        for (int k = 0; k < sinc_table_size; k++) {
            window_table[k] = (float) (-0.5 * Math.cos(2.0 * Math.PI * (double) (k + offset) / (double) sinc_table_size) + 0.5);
        }
        return window_table;
    }

    // Normalized sinc function
    public static double sinc(double x) {
        return x == 0.0 ? 1.0 : Math.sin(Math.PI * x) / (Math.PI * x);
    }

    @Override
    public int getPadding() {
        return sinc_table_center + 2;
    }

    @Override
    public void interpolate(float[] in, AtomicFloat in_offset, float in_end,
                            AtomicFloat pitch, float pitch_step,
                            float[] out, AtomicInteger out_offset, int out_end) {
        int p = Math.min(sinc_scale_size - 1, Math.max(0, (int) ((pitch.get() - 1) * 10)));
        float[][] sinc_table_f = this.sinc_table[p];
        while (in_offset.get() < in_end && out_offset.get() < out_end) {
            int iix = (int) in_offset.get();
            float[] sinc_table = sinc_table_f[(int) ((in_offset.get() - iix) * sinc_table_fsize)];
            int xx = iix - sinc_table_center;
            float y = 0;
            for (int i = 0; i < sinc_table_size; i++, xx++) {
                y += in[xx] * sinc_table[i];
            }
            out[out_offset.getAndIncrement()] = y;
            in_offset.addAndGet(pitch.get());
            if (pitch_step != 0) {
                pitch.addAndGet(pitch_step);
            }
        }
    }
}
