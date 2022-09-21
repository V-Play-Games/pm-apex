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
    private int size;
    private float[] buffer;
    private boolean empty = true;
    private AudioFormat format;
    private AudioFloatConverter converter;
    private byte[] converter_buffer;

    public SoftAudioBuffer(int size, AudioFormat format) {
        this.size = size;
        this.format = format;
        this.converter = AudioFloatConverter.getConverter(format);
    }

    public void swap(SoftAudioBuffer swap) {
        int bak_size = size;
        float[] bak_buffer = buffer;
        boolean bak_empty = empty;
        AudioFormat bak_format = format;
        AudioFloatConverter bak_converter = converter;
        byte[] bak_converter_buffer = converter_buffer;

        size = swap.size;
        buffer = swap.buffer;
        empty = swap.empty;
        format = swap.format;
        converter = swap.converter;
        converter_buffer = swap.converter_buffer;

        swap.size = bak_size;
        swap.buffer = bak_buffer;
        swap.empty = bak_empty;
        swap.format = bak_format;
        swap.converter = bak_converter;
        swap.converter_buffer = bak_converter_buffer;
    }

    public AudioFormat getFormat() {
        return format;
    }

    public int getSize() {
        return size;
    }

    public void clear() {
        if (!empty) {
            Arrays.fill(buffer, 0);
            empty = true;
        }
    }

    public boolean isSilent() {
        return empty;
    }

    public float[] getArray() {
        empty = false;
        if (buffer == null) {
            buffer = new float[size];
        }
        return buffer;
    }

    public void get(byte[] buffer, int channel) {
        int channels = format.getChannels();
        if (channels == 1) {
            converter.copyToByteArray(getArray(), size, buffer);
            return;
        }
        if (channel >= channels) {
            return;
        }
        int frameSize = format.getFrameSize();
        int frameSizePerChannel = frameSize / channels;
        int c_len = size * frameSizePerChannel;
        if (converter_buffer == null || converter_buffer.length < c_len) {
            converter_buffer = new byte[c_len];
        }
        converter.copyToByteArray(getArray(), size, converter_buffer);
        for (int j = 0; j < frameSizePerChannel; j++) {
            int k = j;
            int z = channel * frameSizePerChannel + j;
            for (int i = 0; i < size; i++) {
                buffer[z] = converter_buffer[k];
                z += frameSize;
                k += frameSizePerChannel;
            }
        }
    }
}
