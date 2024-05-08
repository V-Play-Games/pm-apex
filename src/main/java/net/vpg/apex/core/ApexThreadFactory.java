package net.vpg.apex.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.FormatProcessor.FMT;

public class ApexThreadFactory implements ThreadFactory {
    private final String type;
    private final AtomicInteger integer = new AtomicInteger(0);

    public ApexThreadFactory(String type) {
        this.type = type;
    }

    @Override
    public Thread newThread(Runnable r) {
        int threadId = integer.incrementAndGet();
        return new Thread(r, FMT."Apex \{type} Thread: %02d\{threadId}");
    }
}
