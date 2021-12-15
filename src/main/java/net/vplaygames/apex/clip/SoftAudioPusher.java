/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import javax.sound.sampled.SourceDataLine;
import java.io.IOException;

/**
 * This is a processor object that writes into SourceDataLine
 *
 * @author Karl Helgason
 */
public final class SoftAudioPusher implements Runnable {
    private final AudioInputStream ais;
    private final byte[] buffer;
    private volatile boolean active = false;
    private SourceDataLine line;
    private Thread thread;

    public SoftAudioPusher(SourceDataLine line, AudioInputStream ais, int size) {
        this.ais = ais;
        this.buffer = new byte[size];
        this.line = line;
    }

    public synchronized void start() {
        if (active) {
            return;
        }
        active = true;
        thread = new Thread(this, "AudioPusher");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public synchronized void stop() {
        if (!active) return;
        active = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            // ignore
        }
    }

    @Override
    public void run() {
        try {
            while (active) {
                int read = ais.read(buffer);
                if (read < 0) break;
                line.write(buffer, 0, read);
            }
        } catch (IOException e) {
            active = false;
            e.printStackTrace();
        }
    }
}
