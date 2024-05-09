/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.vpg.apex.components;

import net.vpg.apex.core.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

public class Downloader {
    public static final int STARTED = 1;
    public static final int IN_PROGRESS = 2;
    public static final int DONE = 3;

    public static File download(String url) throws IOException {
        return download(url, url.substring(url.lastIndexOf('/') + 1), null);
    }

    public static File download(String url, EventListener listener) throws IOException {
        return download(url, url.substring(url.lastIndexOf('/') + 1), listener);
    }

    public static File download(String url, String filename, EventListener listener) throws IOException {
        return download(url, Resources.getInstance().create(filename), listener);
    }

    public static File download(String url, File file, EventListener listener) throws IOException {
        long startingTime = System.currentTimeMillis();
        int bytesRead = 0;
        int len;
        try (InputStream input = URI.create(url).toURL().openStream()) {
            try (FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                while ((len = input.read(buffer)) > 0) {
                    bytesRead += len;
                    output.write(buffer, 0, len);
                    long timeTaken = System.currentTimeMillis() - startingTime;
                    long speed = bytesRead * 100L / timeTaken;
                    if (listener != null) {
                        listener.progress(new Event(file, startingTime, timeTaken, len, bytesRead, speed, bytesRead == len ? STARTED : IN_PROGRESS));
                    }
                }
            }
        }
        long timeTaken = System.currentTimeMillis() - startingTime;
        long speed = bytesRead * 100L / timeTaken;
        if (listener != null) {
            listener.progress(new Event(file, startingTime, timeTaken, len, bytesRead, speed, DONE));
        }
        return file;
    }

    @FunctionalInterface
    public interface EventListener {
        void progress(Event event);
    }

    public record Event(
        File file,
        long startingTime,
        long timeTaken,
        int bytesRead,
        long totalBytesRead,
        double speed,
        int type
    ) {
    }
}
