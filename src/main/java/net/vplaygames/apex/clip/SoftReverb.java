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
 * Reverb effect based on all pass/comb filters. First audio is send to 8
 * parallel comb filters and then mixed together and then finally send through 3
 * different all pass filters.
 *
 * @author Karl Helgason
 */
public class SoftReverb extends SoftAudioProcessor {
    private static final int[] combs = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
    private float gain = 1;
    private Delay delay;
    private Comb[] combL;
    private Comb[] combR;
    private AllPass[] allpassL;
    private AllPass[] allpassR;
    private float[] input;
    private float[] out;
    private float[] pre1;
    private float[] pre2;
    private float[] pre3;
    private boolean denormal_flip = false;
    private boolean mix = true;
    private SoftAudioBuffer inputA;
    private SoftAudioBuffer left;
    private SoftAudioBuffer right;
    private boolean dirty = true;
    private float dirty_roomsize;
    private float dirty_damp;
    private float dirty_predelay;
    private float dirty_gain;
    private float samplerate;
    private boolean light = true;
    private boolean silent = true;

    public SoftReverb(float sampleRate, float controlRate) {
        super(sampleRate, controlRate);
        this.samplerate = sampleRate;
        int scale = (int) (sampleRate / 44100.0);
        int spread = 23;
        delay = new Delay();

        combL = new Comb[8];
        combR = new Comb[8];
        for (int i = 0; i < combs.length; i++) {
            int size = combs[i];
            combL[i] = new Comb(scale * size);
            combR[i] = new Comb(scale * (size + spread));
        }

        allpassL = new AllPass[4];
        allpassR = new AllPass[4];
        allpassL[0] = new AllPass(scale * 556);
        allpassR[0] = new AllPass(scale * (556 + spread));
        allpassL[1] = new AllPass(scale * 441);
        allpassR[1] = new AllPass(scale * (441 + spread));
        allpassL[2] = new AllPass(scale * 341);
        allpassR[2] = new AllPass(scale * (341 + spread));
        allpassL[3] = new AllPass(scale * 225);
        allpassR[3] = new AllPass(scale * (225 + spread));

        for (int i = 0; i < allpassL.length; i++) {
            allpassL[i].setFeedBack(0.5f);
            allpassR[i].setFeedBack(0.5f);
        }

        /* Init other settings */
        globalParameterControlChange(129, 0, 4);
    }

    @Override
    public void setInput(int pin, SoftAudioBuffer input) {
        if (pin == 0)
            inputA = input;
    }

    @Override
    public void setOutput(int pin, SoftAudioBuffer output) {
        if (pin == 0)
            left = output;
        if (pin == 1)
            right = output;
    }

    @Override
    public void setMixMode(boolean mix) {
        this.mix = mix;
    }

    @Override
    public void processAudio() {
        boolean silent_input = this.inputA.isSilent();
        if (!silent_input)
            silent = false;
        if (silent) {
            if (!mix) {
                left.clear();
                right.clear();
            }
            return;
        }

        float[] inputA = this.inputA.getArray();
        float[] left = this.left.getArray();
        float[] right = this.right == null ? null : this.right.getArray();

        int samples = inputA.length;
        if (input == null || input.length < samples)
            input = new float[samples];

        float again = gain * 0.018f / 2;

        denormal_flip = !denormal_flip;
        float toAdd = denormal_flip ? 1E-20F : -1E-20F;
        for (int i = 0; i < samples; i++)
            input[i] = inputA[i] * again + toAdd;

        delay.processReplace(input);

        if (light && right != null) {
            if (pre1 == null || pre1.length < samples) {
                pre1 = new float[samples];
                pre2 = new float[samples];
                pre3 = new float[samples];
            }

            for (AllPass allPass : allpassL) allPass.processReplace(input);

            combL[0].processMix(input, pre3);
            combL[1].processMix(input, pre3);
            combL[2].processMix(input, pre1);
            combL[3].processMix(input, pre2);
            for (int i = 4; i < combL.length - 2; i++) {
                combL[i++].processMix(input, pre1);
                combL[i].processMix(input, pre2);
            }
            if (!mix) {
                Arrays.fill(right, 0);
                Arrays.fill(left, 0);
            }
            for (int i = combR.length - 2; i < combR.length; i++)
                combR[i].processMix(input, right);
            for (int i = combL.length - 2; i < combL.length; i++)
                combL[i].processMix(input, left);

            for (int i = 0; i < samples; i++) {
                float p = pre1[i] - pre2[i];
                float m = pre3[i];
                left[i] += m + p;
                right[i] += m - p;
            }
        } else {
            if (out == null || out.length < samples)
                out = new float[samples];

            if (right != null) {
                if (!mix)
                    Arrays.fill(right, 0);
                allpassR[0].processReplace(input, out);
                for (int i = 1; i < allpassR.length; i++)
                    allpassR[i].processReplace(out);
                for (Comb comb : combR) comb.processMix(out, right);
            }

            if (!mix)
                Arrays.fill(left, 0);
            allpassL[0].processReplace(input, out);
            for (int i = 1; i < allpassL.length; i++)
                allpassL[i].processReplace(out);
            for (Comb comb : combL) comb.processMix(out, left);
        }

        if (silent_input) {
            silent = true;
            for (int i = 0; i < samples; i++) {
                float v = left[i];
                if (v > 1E-10 || v < -1E-10) {
                    silent = false;
                    break;
                }
            }
        }

    }

    @Override
    public void globalParameterControlChange(int slothPath, int param, int value) {
        if (slothPath != 129) return;
        switch (param) {
            case 0:
                switch (value) {
                    case 0:
                        // Small Room A small size room with a length
                        // of 5m or so.
                        dirty_roomsize = 1.1f;
                        dirty_damp = 5000;
                        dirty_predelay = 0;
                        dirty_gain = 4;
                        dirty = true;
                        break;
                    case 1:
                        // Medium Room A medium size room with a length
                        // of 10m or so.
                        dirty_roomsize = 1.3f;
                        dirty_damp = 5000;
                        dirty_predelay = 0;
                        dirty_gain = 3;
                        dirty = true;
                        break;
                    case 2:
                        // Large Room A large size room suitable for
                        // live performances.
                        dirty_roomsize = 1.5f;
                        dirty_damp = 5000;
                        dirty_predelay = 0;
                        dirty_gain = 2;
                        dirty = true;
                        break;
                    case 3:
                        // Medium Hall A medium size concert hall.
                        dirty_roomsize = 1.8f;
                        dirty_damp = 24000;
                        dirty_predelay = 0.02f;
                        dirty_gain = 1.5f;
                        dirty = true;
                        break;
                    case 4:
                        // Large Hall A large size concert hall
                        // suitable for a full orchestra.
                        dirty_roomsize = 1.8f;
                        dirty_damp = 24000;
                        dirty_predelay = 0.03f;
                        dirty_gain = 1.5f;
                        dirty = true;
                        break;
                    case 8:
                        // Plate A plate reverb simulation.
                        dirty_roomsize = 1.3f;
                        dirty_damp = 2500;
                        dirty_predelay = 0;
                        dirty_gain = 6;
                        dirty = true;
                        break;
                }
                break;
            case 1:
                dirty_roomsize = (float) Math.exp((value - 40) * 0.025);
                dirty = true;
                break;
        }
    }

    @Override
    public void processControlLogic() {
        if (dirty) {
            dirty = false;
            setRoomSize(dirty_roomsize);
            setDamp(dirty_damp);
            setPreDelay(dirty_predelay);
            setGain(dirty_gain);
        }
    }

    public void setRoomSize(float value) {
        float roomSize = 1 - 0.17f / value;
        for (int i = 0; i < combL.length; i++) {
            combL[i].setFeedBack(roomSize);
            combR[i].setFeedBack(roomSize);
        }
    }

    public void setPreDelay(float value) {
        delay.setDelay((int) (value * samplerate));
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public void setDamp(float value) {
        double x = (value / samplerate) * (2 * Math.PI);
        double cx = 2 - Math.cos(x);
        float damp = Math.min(1, Math.max(0, (float) (cx - Math.sqrt(cx * cx - 1))));
        for (int i = 0; i < combL.length; i++) {
            combL[i].setDamp(damp);
            combR[i].setDamp(damp);
        }
    }

    public void setLightMode(boolean light) {
        this.light = light;
    }

    private static final class Delay {
        private float[] delayBuffer;
        private int position = 0;

        Delay() {
            delayBuffer = null;
        }

        public void setDelay(int delay) {
            if (delay == 0)
                delayBuffer = null;
            else
                delayBuffer = new float[delay];
            position = 0;
        }

        public void processReplace(float[] input) {
            if (delayBuffer == null)
                return;
            for (int i = 0; i < input.length; i++) {
                float x = input[i];
                input[i] = delayBuffer[position];
                delayBuffer[position] = x;
                if (++position == delayBuffer.length)
                    position = 0;
            }
        }
    }

    private static final class AllPass {

        private final float[] delaybuffer;
        private final int delaybuffersize;
        private int rovepos = 0;
        private float feedback;

        AllPass(int size) {
            delaybuffer = new float[size];
            delaybuffersize = size;
        }

        public void setFeedBack(float feedback) {
            this.feedback = feedback;
        }

        public void processReplace(float[] inout) {
            int len = inout.length;
            int delaybuffersize = this.delaybuffersize;
            int rovepos = this.rovepos;
            for (int i = 0; i < len; i++) {
                float delayout = delaybuffer[rovepos];
                float input = inout[i];
                inout[i] = delayout - input;
                delaybuffer[rovepos] = input + delayout * feedback;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
            this.rovepos = rovepos;
        }

        public void processReplace(float[] in, float[] out) {
            int len = in.length;
            int delaybuffersize = this.delaybuffersize;
            int rovepos = this.rovepos;
            for (int i = 0; i < len; i++) {
                float delayout = delaybuffer[rovepos];
                float input = in[i];
                out[i] = delayout - input;
                delaybuffer[rovepos] = input + delayout * feedback;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
            this.rovepos = rovepos;
        }
    }

    private static final class Comb {
        private final float[] delayBuffer;
        private final int delaybuffersize;
        private int rovepos = 0;
        private float feedback;
        private float filtertemp = 0;
        private float filtercoeff1 = 0;
        private float filtercoeff2 = 1;

        Comb(int size) {
            delayBuffer = new float[size];
            delaybuffersize = size;
        }

        public void setFeedBack(float feedback) {
            this.feedback = feedback;
            filtercoeff2 = (1 - filtercoeff1) * feedback;
        }

        public void setDamp(float val) {
            filtercoeff1 = val;
            filtercoeff2 = (1 - filtercoeff1) * feedback;
        }

        public void processMix(float[] in, float[] out) {
            for (int i = 0; i < in.length; i++) {
                float delay = delayBuffer[rovepos];
                // One Pole Lowpass Filter
                filtertemp = delay * filtercoeff2 + filtertemp * filtercoeff1;
                out[i] += delay;
                delayBuffer[rovepos] = in[i] + filtertemp;
                if (++rovepos == delaybuffersize)
                    rovepos = 0;
            }
        }
    }
}

