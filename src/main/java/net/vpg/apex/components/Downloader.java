package net.vpg.apex.components;

import net.vpg.apex.core.Resources;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

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
        long bytesRead = 0;
        int len;
        try (InputStream input = new URL(url).openStream()) {
            try (FileOutputStream output = new FileOutputStream(file)) {
                byte[] buffer = new byte[4096];
                while ((len = input.read(buffer)) > 0) {
                    bytesRead += len;
                    output.write(buffer, 0, len);
                    long timeTaken = System.currentTimeMillis() - startingTime;
                    long speed = bytesRead * 100 / timeTaken;
                    if (listener != null) {
                        listener.progress(new Event(file, startingTime, timeTaken, len, bytesRead, speed, bytesRead == len ? STARTED : IN_PROGRESS));
                    }
                }
            }
        }
        long timeTaken = System.currentTimeMillis() - startingTime;
        long speed = bytesRead * 100 / timeTaken;
        if (listener != null) {
            listener.progress(new Event(file, startingTime, timeTaken, len, bytesRead, speed, DONE));
        }
        return file;
    }

    @FunctionalInterface
    public interface EventListener {
        void progress(Event event);
    }

    public static class Event {
        public final File file;
        public final long startingTime;
        public final long timeTaken;
        public final long bytesRead;
        public final long totalBytesRead;
        public final double speed;
        public final int type;

        public Event(File file, long startingTime, long timeTaken, long bytesRead, long totalBytesRead, double speed, int type) {
            this.file = file;
            this.startingTime = startingTime;
            this.timeTaken = timeTaken;
            this.bytesRead = bytesRead;
            this.totalBytesRead = totalBytesRead;
            this.speed = speed;
            this.type = type;
        }
    }
}
