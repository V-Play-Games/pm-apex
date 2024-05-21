package net.vpg.apex.core;

import javax.swing.*;
import java.util.List;

public class ApexPlaylist {
    private final String category;
    private final List<Track> tracks;
    private final DefaultListModel<String> model;

    public ApexPlaylist(String category, List<Track> tracks) {
        this.category = category;
        this.tracks = tracks;
        model = new DefaultListModel<>();
        model.addAll(tracks.stream().map(Track::name).toList());
    }

    public String getCategory() {
        return category;
    }

    public Track get(int index) {
        return tracks.get(index);
    }

    public int size() {
        return tracks.size();
    }

    public DefaultListModel<String> getModel() {
        return model;
    }
}
