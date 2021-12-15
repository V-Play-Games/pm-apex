/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class Toolkit {
    private Toolkit() {
        // Suppresses default constructor
    }

    /**
     * Throws an exception if the buffer size does not represent an integral
     * number of sample frames.
     */
    public static void validateBuffer(int frameSize, int bufferSize) {
        if (bufferSize % frameSize != 0)
            throw new IllegalArgumentException(String.format("Buffer size (%d) does not represent an integral number of sample frames (%d)", bufferSize, frameSize));
    }

    public static byte[] cache(AudioInputStream stream) throws IOException {
        int frameSize = stream.getFormat().getFrameSize();
        if (stream.getFrameLength() != AudioSystem.NOT_SPECIFIED) {
            byte[] data = new byte[(int) stream.getFrameLength() * frameSize];
            int off = 0, read;
            while ((read = stream.read(data, off, data.length)) != -1)
                off += read;
            return Arrays.copyOf(data, off);
        } else {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[512 * frameSize];
            int read;
            while ((read = stream.read(buffer)) != -1)
                outputStream.write(buffer, 0, read);
            return outputStream.toByteArray();
        }
    }
}
