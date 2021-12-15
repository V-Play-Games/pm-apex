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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * This class is used to create AudioFloatInputStream from AudioInputStream and
 * byte buffers.
 *
 * @author Karl Helgason
 */
public abstract class AudioFloatInputStream {
    public static AudioFloatInputStream getInputStream(URL url) throws UnsupportedAudioFileException, IOException {
        return new DirectAudioFloatInputStream(AudioSystem.getAudioInputStream(url));
    }

    public static AudioFloatInputStream getInputStream(File file) throws UnsupportedAudioFileException, IOException {
        return new DirectAudioFloatInputStream(AudioSystem.getAudioInputStream(file));
    }

    public static AudioFloatInputStream getInputStream(InputStream stream) throws UnsupportedAudioFileException, IOException {
        return new DirectAudioFloatInputStream(AudioSystem.getAudioInputStream(stream));
    }

    public static AudioFloatInputStream getInputStream(AudioInputStream stream) {
        return new DirectAudioFloatInputStream(stream);
    }

    public static AudioFloatInputStream getInputStream(AudioFormat format, byte[] buffer, int offset, int len) {
        AudioFloatConverter converter = AudioFloatConverter.getConverter(format);
        if (converter != null) {
            return new ByteArrayAudioFloatInputStream(converter, format, buffer, offset, len);
        }
        InputStream stream = new ByteArrayInputStream(buffer, offset, len);
        long aLen = format.getFrameSize() == AudioSystem.NOT_SPECIFIED ? AudioSystem.NOT_SPECIFIED : len / format.getFrameSize();
        AudioInputStream ais = new AudioInputStream(stream, format, aLen);
        return getInputStream(ais);
    }

    public abstract AudioFormat getFormat();

    public abstract long getFrameLength();

    public abstract int read(float[] b, int off, int len) throws IOException;

    public final int read(float[] b) throws IOException {
        return read(b, 0, b.length);
    }

    public final float read() throws IOException {
        float[] b = new float[1];
        int ret = read(b, 0, 1);
        if (ret == -1 || ret == 0) return 0;
        return b[0];
    }

    public abstract long skip(long len) throws IOException;

    public abstract int available() throws IOException;

    public abstract void close() throws IOException;

    public abstract void mark(int limit);

    public abstract boolean markSupported();

    public abstract void reset() throws IOException;

    private static class ByteArrayAudioFloatInputStream extends AudioFloatInputStream {
        private final AudioFloatConverter converter;
        private final AudioFormat format;
        private final byte[] buffer;
        private final int buffer_offset;
        private final int buffer_len;
        private final int frameSizePerChannel;
        private int pos = 0;
        private int markedPos = 0;

        ByteArrayAudioFloatInputStream(AudioFloatConverter converter, AudioFormat format, byte[] buffer, int offset, int len) {
            this.converter = converter;
            this.format = format;
            this.buffer = buffer;
            this.buffer_offset = offset;
            frameSizePerChannel = format.getFrameSize() / format.getChannels();
            this.buffer_len = len / frameSizePerChannel;
        }

        @Override
        public AudioFormat getFormat() {
            return format;
        }

        @Override
        public long getFrameLength() {
            return buffer_len;
        }

        @Override
        public int read(float[] b, int off, int len) {
            if (b == null) throw new NullPointerException();
            if (off < 0 || len < 0 || len > b.length - off) throw new IndexOutOfBoundsException();
            if (pos >= buffer_len) return -1;
            if (len == 0) return 0;
            if (pos + len > buffer_len) len = buffer_len - pos;
            converter.toFloatArray(buffer, buffer_offset + pos * frameSizePerChannel, b, off, len);
            pos += len;
            return len;
        }

        @Override
        public long skip(long len) {
            if (pos >= buffer_len) return -1;
            if (len <= 0) return 0;
            if (pos + len > buffer_len) len = buffer_len - pos;
            pos += len;
            return len;
        }

        @Override
        public int available() {
            return buffer_len - pos;
        }

        @Override
        public void close() {
        }

        @Override
        public void mark(int limit) {
            markedPos = pos;
        }

        @Override
        public boolean markSupported() {
            return true;
        }

        @Override
        public void reset() {
            pos = markedPos;
        }
    }

    private static class DirectAudioFloatInputStream extends AudioFloatInputStream {
        private final AudioInputStream stream;
        private final int frameSizePerChannel;
        private AudioFloatConverter converter;
        private byte[] buffer;

        private DirectAudioFloatInputStream(AudioInputStream stream) {
            converter = AudioFloatConverter.getConverter(stream.getFormat());
            if (converter == null) {
                AudioFormat format = stream.getFormat();
                AudioFormat[] formats = AudioSystem.getTargetFormats(AudioFormat.Encoding.PCM_SIGNED, format);
                AudioFormat newFormat;
                if (formats.length != 0) {
                    newFormat = formats[0];
                } else {
                    float sampleRate = format.getSampleRate();
                    int sampleSizeInBits = 16;
                    int frameSize = format.getChannels() * (sampleSizeInBits / 8);
                    newFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, sampleRate, sampleSizeInBits, format.getChannels(), frameSize, sampleRate, false);
                }
                stream = AudioSystem.getAudioInputStream(newFormat, stream);
                converter = AudioFloatConverter.getConverter(newFormat);
            }
            frameSizePerChannel = stream.getFormat().getFrameSize() / stream.getFormat().getChannels();
            this.stream = stream;
        }

        @Override
        public AudioFormat getFormat() {
            return stream.getFormat();
        }

        @Override
        public long getFrameLength() {
            return stream.getFrameLength();
        }

        @Override
        public int read(float[] b, int off, int len) throws IOException {
            int b_len = len * frameSizePerChannel;
            if (buffer == null || buffer.length < b_len) buffer = new byte[b_len];
            int read = stream.read(buffer, 0, b_len);
            if (read == -1) return -1;
            converter.toFloatArray(buffer, b, off, read / frameSizePerChannel);
            return read / frameSizePerChannel;
        }

        @Override
        public long skip(long len) throws IOException {
            return stream.skip(len * frameSizePerChannel) / frameSizePerChannel;
        }

        @Override
        public int available() throws IOException {
            return stream.available() / frameSizePerChannel;
        }

        @Override
        public void close() throws IOException {
            stream.close();
        }

        @Override
        public void mark(int limit) {
            stream.mark(limit * frameSizePerChannel);
        }

        @Override
        public boolean markSupported() {
            return stream.markSupported();
        }

        @Override
        public void reset() throws IOException {
            stream.reset();
        }
    }
}
