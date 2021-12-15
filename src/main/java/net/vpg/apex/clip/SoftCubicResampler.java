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
 * A resampler that uses third-order (cubic) interpolation.
 *
 * @author Karl Helgason
 */
public final class SoftCubicResampler extends SoftAbstractResampler {
    @Override
    public int getPadding() {
        return 3;
    }

    @Override
    public void interpolate(float[] in, AtomicFloat in_offset, float in_end,
                            AtomicFloat startPitch, float pitch_step,
                            float[] out, AtomicInteger out_offset, int out_end) {
        while (in_offset.get() < in_end && out_offset.get() < out_end) {
            int iix = in_offset.intValue();
            float fix = in_offset.get() - iix;
            float y0 = in[iix - 1];
            float y1 = in[iix];
            float y2 = in[iix + 1];
            float y3 = in[iix + 2];
            float a0 = y3 - y2 + y1 - y0;
            float a1 = y0 - y1 - a0;
            float a2 = y2 - y0;
            out[out_offset.getAndIncrement()] = ((a0 * fix + a1) * fix + a2) * fix + y1;
            in_offset.addAndGet(startPitch.get());
            if (pitch_step != 0) {
                startPitch.addAndGet(pitch_step);
            }
        }

    }
}
