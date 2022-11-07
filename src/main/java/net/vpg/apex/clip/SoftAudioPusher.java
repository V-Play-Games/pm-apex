package net.vpg.apex.clip;

import net.vpg.apex.Util;

import javax.sound.sampled.SourceDataLine;
import java.io.IOException;
import java.io.InputStream;

public class SoftAudioPusher {
    private final SourceDataLine line;
    private final InputStream stream;
    private final byte[] buffer;
    private volatile boolean active = false;
    private Thread thread;

    public SoftAudioPusher(SourceDataLine line, InputStream stream) {
        this.line = line;
        this.stream = stream;
        this.buffer = new byte[4096];
    }

    public void start() {
        if (active) return;
        active = true;
        thread = new Thread(this::push, "AudioPusher");
        thread.setDaemon(true);
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
    }

    public void stop() {
        if (!active) return;
        active = false;
        Util.run(thread::join);
        thread = null;
    }

    private void push() {
        try {
            while (active) {
                int read = stream.read(buffer);
                if (read < 0) break;
                line.write(buffer, 0, read);
            }
        } catch (IOException e) {
            active = false;
            e.printStackTrace();
        }
    }
}
