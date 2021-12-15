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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A resampler that uses first-order (linear) interpolation.
 * <p>
 * This one doesn't perform float to int casting inside the processing loop.
 *
 * @author Karl Helgason
 */
public class SoftLinearResampler2 extends SoftAbstractResampler {
    @Override
    public int getPadding() {
        return 2;
    }

    @Override
    public void interpolate(float[] in, AtomicFloat in_offset, float in_end,
                            AtomicFloat pitch, float pitch_step,
                            float[] out, AtomicInteger out_offset, int out_end) {
        // Check if we have to do anything
        if (!(in_offset.get() < in_end && out_offset.get() < out_end))
            return;

        // 15 bit shift was chosen because
        // it resulted in no drift between p_ix and in_offset.
        int shift = 1 << 15;

        int p_ix = (int) (in_offset.get() * shift);
        int p_ix_end = (int) (in_end * shift);
        int p_pitch = (int) (pitch.get() * shift);
        // Pitch needs to be recalculated
        // to ensure no drift between p_ix and in_offset.
        pitch.set(p_pitch / (float) shift);

        int p_step = (int) (pitch_step * shift);
        // To reduce
        //    while (p_ix < p_ix_end && out_offset < out_end)
        // into
        //    while (out_offset < out_end)
        // We need to calculate new out_end value.
        out_end = Math.min(out_end, out_offset.get() + (p_ix_end - p_ix) / p_pitch);

        while (out_offset.get() < out_end) {
            int iix = p_ix >> 15;
            float fix = in_offset.get() - iix;
            float i = in[iix];
            out[out_offset.getAndIncrement()] = i + (in[iix + 1] - i) * fix;
            in_offset.addAndGet(pitch.get());
            p_ix += p_pitch;
            if (pitch_step != 0f) {
                pitch.addAndGet(pitch_step);
                p_pitch += p_step;
            }
        }
    }
}
