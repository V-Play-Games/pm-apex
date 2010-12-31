package net.vpg.apex.core;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class ApexThreadFactory implements ThreadFactory {
    AtomicInteger integer = new AtomicInteger(0);

    @Override
    public Thread newThread(Runnable r) {
        int threadId = integer.incrementAndGet();
        return new Thread(r, "Apex Thread: " + (threadId < 10 ? "0" : "") + threadId);
    }
}
