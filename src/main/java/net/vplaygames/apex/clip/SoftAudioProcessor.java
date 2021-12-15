/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Audio processor interface.
 *
 * @author Karl Helgason
 */
public abstract class SoftAudioProcessor {
    protected final float sampleRate;
    protected final float controlRate;

    protected SoftAudioProcessor(float sampleRate, float controlRate) {
        this.sampleRate = sampleRate;
        this.controlRate = controlRate;
    }

    abstract void globalParameterControlChange(int slothPath, int param, int value);

    abstract void setInput(int pin, SoftAudioBuffer input);

    abstract void setOutput(int pin, SoftAudioBuffer output);

    abstract void setMixMode(boolean mix);

    abstract void processAudio();

    abstract void processControlLogic();
}
