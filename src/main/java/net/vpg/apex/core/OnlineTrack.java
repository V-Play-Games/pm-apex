package net.vpg.apex.core;

public class OnlineTrack {
    public final String name;
    public final long size;

    public OnlineTrack(String name, long size) {
        this.name = name;
        this.size = size;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }
}
