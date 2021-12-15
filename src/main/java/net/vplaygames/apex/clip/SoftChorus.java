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

import java.util.Arrays;

/**
 * A chorus effect made using LFO and variable delay. One for each channel
 * (left,right), with different starting phase for stereo effect.
 *
 * @author Karl Helgason
 */
public final class SoftChorus extends SoftAudioProcessor {
    double silentCounter = 1000;
    private boolean mix = true;
    private SoftAudioBuffer input;
    private SoftAudioBuffer left;
    private SoftAudioBuffer right;
    private SoftAudioBuffer reverb;
    private LFODelay delayL;
    private LFODelay delayR;
    private float rGain = 0;
    private boolean dirty = true;
    private double dirty_delayL_rate;
    private double dirty_delayR_rate;
    private double dirty_delayL_depth;
    private double dirty_delayR_depth;
    private float dirty_delayL_feedback;
    private float dirty_delayR_feedback;
    private float dirty_delayL_reverbSendGain;
    private float dirty_delayR_reverbSendGain;
    private float controlRate;

    public SoftChorus(float sampleRate, float controlRate) {
        super(sampleRate, controlRate);
        this.controlRate = controlRate;
        delayL = new LFODelay(sampleRate, controlRate);
        delayR = new LFODelay(sampleRate, controlRate);
        delayL.setGain(1.0f); // %
        delayR.setGain(1.0f); // %
        delayL.setPhase(0.5 * Math.PI);
        delayR.setPhase(0);

        globalParameterControlChange(130, 0, 2);
    }

    @Override
    public void globalParameterControlChange(int slothPath, int param, int value) {
        if (slothPath != 130) {
            return;
        }
        switch (param) { // Chorus Type
            case 0:
                switch (value) {
                    case 0: // Chorus 1 0 (0%) 3 (0.4Hz) 5 (1.9ms) 0 (0%)
                        globalParameterControlChange(slothPath, 3, 0);
                        globalParameterControlChange(slothPath, 1, 3);
                        globalParameterControlChange(slothPath, 2, 5);
                        globalParameterControlChange(slothPath, 4, 0);
                        break;
                    case 1: // Chorus 2 5 (4%) 9 (1.1Hz) 19 (6.3ms) 0 (0%)
                        globalParameterControlChange(slothPath, 3, 5);
                        globalParameterControlChange(slothPath, 1, 9);
                        globalParameterControlChange(slothPath, 2, 19);
                        globalParameterControlChange(slothPath, 4, 0);
                        break;
                    case 2: // Chorus 3 8 (6%) 3 (0.4Hz) 19 (6.3ms) 0 (0%)
                        globalParameterControlChange(slothPath, 3, 8);
                        globalParameterControlChange(slothPath, 1, 3);
                        globalParameterControlChange(slothPath, 2, 19);
                        globalParameterControlChange(slothPath, 4, 0);
                        break;
                    case 3: // Chorus 4 16 (12%) 9 (1.1Hz) 16 (5.3ms) 0 (0%)
                        globalParameterControlChange(slothPath, 3, 16);
                        globalParameterControlChange(slothPath, 1, 9);
                        globalParameterControlChange(slothPath, 2, 16);
                        globalParameterControlChange(slothPath, 4, 0);
                        break;
                    case 4: // FB Chorus 64 (49%) 2 (0.2Hz) 24 (7.8ms) 0 (0%)
                        globalParameterControlChange(slothPath, 3, 64);
                        globalParameterControlChange(slothPath, 1, 2);
                        globalParameterControlChange(slothPath, 2, 24);
                        globalParameterControlChange(slothPath, 4, 0);
                        break;
                    case 5: // Flanger 112 (86%) 1 (0.1Hz) 5 (1.9ms) 0 (0%)
                        globalParameterControlChange(slothPath, 3, 112);
                        globalParameterControlChange(slothPath, 1, 1);
                        globalParameterControlChange(slothPath, 2, 5);
                        globalParameterControlChange(slothPath, 4, 0);
                        break;
                    default:
                        break;
                }
                break;
            case 1: // Mod Rate
                dirty_delayL_rate = value * 0.122;
                dirty_delayR_rate = value * 0.122;
                dirty = true;
                break;
            case 2:  // Mod Depth
                dirty_delayL_depth = (value + 1) / 3200.0;
                dirty_delayR_depth = (value + 1) / 3200.0;
                dirty = true;
                break;
            case 3:  // Feedback
                dirty_delayL_feedback = value * 0.00763f;
                dirty_delayR_feedback = value * 0.00763f;
                dirty = true;
                break;
            case 4:  // Send to Reverb
                rGain = value * 0.00787f;
                dirty_delayL_reverbSendGain = value * 0.00787f;
                dirty_delayR_reverbSendGain = value * 0.00787f;
                dirty = true;
                break;
        }
    }

    @Override
    public void processControlLogic() {
        if (dirty) {
            dirty = false;
            delayL.setRate(dirty_delayL_rate);
            delayR.setRate(dirty_delayR_rate);
            delayL.setDepth(dirty_delayL_depth);
            delayR.setDepth(dirty_delayR_depth);
            delayL.setFeedBack(dirty_delayL_feedback);
            delayR.setFeedBack(dirty_delayR_feedback);
            delayL.setReverbSendGain(dirty_delayL_reverbSendGain);
            delayR.setReverbSendGain(dirty_delayR_reverbSendGain);
        }
    }

    @Override
    public void processAudio() {
        if (input.isSilent()) {
            silentCounter += 1 / controlRate;
            if (silentCounter > 1) {
                if (!mix) {
                    left.clear();
                    right.clear();
                }
                return;
            }
        } else {
            silentCounter = 0;
        }

        float[] input = this.input.getArray();
        float[] left = this.left.getArray();
        float[] right = this.right == null ? null : this.right.getArray();
        float[] reverb = rGain != 0 ? this.reverb.getArray() : null;

        if (mix) {
            delayL.processMix(input, left, reverb);
            if (right != null)
                delayR.processMix(input, right, reverb);
        } else {
            delayL.processReplace(input, left, reverb);
            if (right != null)
                delayR.processReplace(input, right, reverb);
        }
    }

    @Override
    public void setInput(int pin, SoftAudioBuffer input) {
        if (pin == 0)
            this.input = input;
    }

    @Override
    public void setMixMode(boolean mix) {
        this.mix = mix;
    }

    @Override
    public void setOutput(int pin, SoftAudioBuffer output) {
        if (pin == 0)
            left = output;
        if (pin == 1)
            right = output;
        if (pin == 2)
            reverb = output;
    }

    private static class VariableDelay {
        private final float[] buffer;
        private int rovePos = 0;
        private float gain = 1;
        private float rGain = 0;
        private float delay = 0;
        private float lastDelay = 0;
        private float feedback = 0;

        VariableDelay(int size) {
            buffer = new float[size];
        }

        public void setDelay(float delay) {
            this.delay = delay;
        }

        public void setFeedBack(float feedback) {
            this.feedback = feedback;
        }

        public void setGain(float gain) {
            this.gain = gain;
        }

        public void setReverbSendGain(float rGain) {
            this.rGain = rGain;
        }

        public void processMix(float[] in, float[] out, float[] rout) {
            int len = in.length;
            float delayDelta = (delay - lastDelay) / len;
            int delayLen = buffer.length;

            for (int i = 0; i < len; i++) {
                float r = rovePos - (lastDelay + 2) + delayLen;
                int ri = (int) r;
                float s = r - ri;
                float a = buffer[ri % delayLen];
                float b = buffer[(ri + 1) % delayLen];
                float o = a * (1 - s) + b * s;
                out[i] += o * gain;
                if (rout != null) {
                    rout[i] += o * rGain;
                }
                buffer[rovePos] = in[i] + o * feedback;
                rovePos = (rovePos + 1) % delayLen;
                lastDelay += delayDelta;
            }
            lastDelay = delay;
        }

        public void processReplace(float[] in, float[] out, float[] rout) {
            Arrays.fill(out, 0);
            Arrays.fill(rout, 0);
            processMix(in, out, rout);
        }
    }

    private static class LFODelay {
        private final double sampleRate;
        private final double controlRate;
        private double phase = 1;
        private double phase_step = 0;
        private double depth = 0;
        private VariableDelay delay;

        LFODelay(double sampleRate, double controlRate) {
            this.sampleRate = sampleRate;
            this.controlRate = controlRate;
            delay = new VariableDelay((int) ((this.depth + 10) * 2));
        }

        public void setDepth(double depth) {
            this.depth = depth * sampleRate;
            delay = new VariableDelay((int) ((this.depth + 10) * 2));
        }

        public void setRate(double rate) {
            phase_step = Math.PI * 2 * (rate / controlRate);
        }

        public void setPhase(double phase) {
            this.phase = phase;
        }

        public void setFeedBack(float feedback) {
            delay.setFeedBack(feedback);
        }

        public void setGain(float gain) {
            delay.setGain(gain);
        }

        public void setReverbSendGain(float gain) {
            delay.setReverbSendGain(gain);
        }

        public void processMix(float[] in, float[] out, float[] rout) {
            phase = (phase + phase_step) % (Math.PI * 2);
            delay.setDelay((float) (depth * 0.5 * (Math.cos(phase) + 2)));
            delay.processMix(in, out, rout);
        }

        public void processReplace(float[] in, float[] out, float[] rout) {
            phase += phase_step;
            while (phase > Math.PI * 2) phase -= Math.PI * 2;
            delay.setDelay((float) (depth * 0.5 * (Math.cos(phase) + 2)));
            delay.processReplace(in, out, rout);
        }
    }
}
