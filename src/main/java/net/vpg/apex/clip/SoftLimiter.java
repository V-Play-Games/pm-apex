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

/**
 * A simple look-ahead volume limiter with very fast attack and fast release.
 * This filter is used for preventing clipping.
 *
 * @author Karl Helgason
 */
public final class SoftLimiter extends SoftAudioProcessor {
    float lastMax = 0;
    float gain = 1;
    float[] temp_bufferL;
    float[] temp_bufferR;
    boolean mix = false;
    SoftAudioBuffer bufferL;
    SoftAudioBuffer bufferR;
    SoftAudioBuffer bufferLout;
    SoftAudioBuffer bufferRout;
    float sampleRate;
    double silentCounter = 0;

    public SoftLimiter(float sampleRate, float controlRate) {
        super(sampleRate, controlRate);
        this.sampleRate = sampleRate;
    }

    @Override
    public void setInput(int pin, SoftAudioBuffer input) {
        if (pin == 0)
            bufferL = input;
        if (pin == 1)
            bufferR = input;
    }

    @Override
    public void setOutput(int pin, SoftAudioBuffer output) {
        if (pin == 0)
            bufferLout = output;
        if (pin == 1)
            bufferRout = output;
    }

    @Override
    public void setMixMode(boolean mix) {
        this.mix = mix;
    }

    @Override
    public void globalParameterControlChange(int slothPath, int param, int value) {
    }

    @Override
    public void processAudio() {
        if (bufferL.isSilent() && (bufferR == null || bufferR.isSilent())) {
            silentCounter += 1 / sampleRate;
            if (silentCounter > 60) {
                if (!mix) {
                    bufferLout.clear();
                    if (bufferRout != null) bufferRout.clear();
                }
                return;
            }
        } else
            silentCounter = 0;

        float[] bufferL = this.bufferL.getArray();
        float[] bufferR = this.bufferR == null ? null : this.bufferR.getArray();
        float[] bufferLout = this.bufferLout.getArray();
        float[] bufferRout = this.bufferRout == null ? null : this.bufferRout.getArray();

        if (temp_bufferL == null || temp_bufferL.length < bufferL.length)
            temp_bufferL = new float[bufferL.length];
        if (bufferR != null)
            if (temp_bufferR == null || temp_bufferR.length < bufferR.length)
                temp_bufferR = new float[bufferR.length];

        float max = 0;
        int len = bufferL.length;

        if (bufferR == null) {
            for (float v : bufferL) {
                if (v > max)
                    max = v;
                if (-v > max)
                    max = -v;
            }
        } else {
            for (int i = 0; i < len; i++) {
                if (bufferL[i] > max)
                    max = bufferL[i];
                if (bufferR[i] > max)
                    max = bufferR[i];
                if (-bufferL[i] > max)
                    max = -bufferL[i];
                if (-bufferR[i] > max)
                    max = -bufferR[i];
            }
        }

        float last_max = lastMax;
        lastMax = max;
        if (last_max > max)
            max = last_max;

        float newGain = (max > 0.99f) ? 0.99f / max : 1;

        if (newGain > gain)
            newGain = (newGain + gain * 9) / 10f;

        float gainChange = (newGain - gain) / len;
        for (int i = 0; i < len; i++) {
            gain += gainChange;
            float bL = bufferL[i];
            float tL = temp_bufferL[i];
            temp_bufferL[i] = bL;
            bufferLout[i] = (mix ? bufferLout[i] : 0) + (tL * gain);
            if (bufferR != null) {
                float bR = bufferR[i];
                float tR = temp_bufferR[i];
                temp_bufferR[i] = bR;
                //noinspection ConstantConditions
                bufferRout[i] = (mix ? bufferRout[i] : 0) + (tR * gain);
            }
        }
        gain = newGain;
    }

    @Override
    public void processControlLogic() {
    }
}
