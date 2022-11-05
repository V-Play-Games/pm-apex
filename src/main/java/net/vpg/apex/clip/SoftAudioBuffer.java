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

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;

/**
 * This class is used to store audio buffer.
 *
 * @author Karl Helgason
 */
public final class SoftAudioBuffer {
    private final int size;
    private final AudioFormat format;
    private final AudioFloatConverter converter;
    private final float[] source;
    private byte[] buffer;

    public SoftAudioBuffer(int size, AudioFormat format) {
        this.size = size;
        this.source = new float[size];
        this.format = format;
        this.converter = AudioFloatConverter.getConverter(format);
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        Arrays.fill(source, 0);
    }

    public float[] getArray() {
        return source;
    }

    public void get(byte[] dest, int channel) {
        int channels = format.getChannels();
        if (channel >= channels) {
            return;
        }
        if (channels == 1) {
            converter.copyToByteArray(source, dest);
            return;
        }
        int frameSize = format.getFrameSize();
        int frameSizePerChannel = frameSize / channels;
        if (buffer == null) {
            buffer = new byte[size * frameSizePerChannel];
        }
        converter.copyToByteArray(source, buffer);
        for (int i = 0; i < frameSizePerChannel; i++) {
            for (int j = 0, k = i, z = channel * frameSizePerChannel + i;
                 j < size;
                 j++, z += frameSize, k += frameSizePerChannel) {
                dest[z] = buffer[k];
            }
        }
    }
}
