package net.vpg.apex.core;

import javax.swing.*;
import java.util.List;

public class ApexPlaylist {
    public static final ApexPlaylist EMPTY = new ApexPlaylist("", List.of());
    private final String category;
    private final List<ApexTrack> tracks;
    private final DefaultListModel<String> model;

    public ApexPlaylist(String category, List<ApexTrack> tracks) {
        this.category = category;
        this.tracks = tracks;
        model = new DefaultListModel<>();
        model.addAll(tracks.stream().map(ApexTrack::name).toList());
    }

    public String getCategory() {
        return category;
    }

    public ApexTrack get(int index) {
        return tracks.get(index);
    }

    public int size() {
        return tracks.size();
    }

    public DefaultListModel<String> getModel() {
        return model;
    }
}
