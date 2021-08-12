package net.vplaygames.apex;

import net.vplaygames.apex.components.ApexControl;
import net.vplaygames.apex.components.ApexWindow;
import net.vplaygames.apex.core.Resources;
import net.vplaygames.apex.core.Track;

import java.util.*;
import java.util.stream.Collectors;

import static net.vplaygames.apex.components.ApexControl.*;

public class Apex {
    public static final Apex apex = new Apex();
    ApexWindow window;
    List<Track> playlist = new ArrayList<>();
    int index = 0;

    private void start() throws Exception {
        ApexControl.init();
        window = new ApexWindow();
        updatePlaylist();
        changeTrack(0);
        ApexControl.update();
    }

    public static void main(String[] args) throws Exception {
        apex.start();
    }

    private void updatePlaylist() {
        playlist = Resources.getResources()
            .values()
            .stream()
            .filter(f -> f.getName().endsWith(".ogg"))
            .map(Track::get)
            .sorted(Comparator.comparing(Track::getId))
            .collect(Collectors.toList());
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public int getIndex() {
        return index;
    }

    public void takeAction(int action) {
        synchronized (this) {
            Track track = getPlaylist().get(getIndex());
            switch (action) {
                case 0:
                    changeTrack(1);
                    playing = true;
                    stopped = false;
                    break;
                case 1:
                    changeTrack(-1);
                    playing = true;
                    stopped = false;
                    break;
                case 2:
                    playlist = shuffle(playlist);
                    index = indexOf(track);
                    break;
                case 3:
                    stopCurrentTrack();
                    playing = false;
                    stopped = true;
                    break;
                case 4:
                    playing = track.getClip().isActive();
                    if (playing) {
                        track.stop();
                    } else {
                        track.play();
                        stopped = false;
                    }
                    playing = !playing;
                    break;
                case 5:
                    String searchText = searchTextArea.getText().toLowerCase();
                    for (int i = 0; i < playlist.size(); i++) {
                        Track t = playlist.get(i);
                        if (t.getName().toLowerCase().contains(searchText) || t.getId().contains(searchText)) {
                            changeTrack(i - index);
                            break;
                        }
                    }
                    break;
                case 6:
                    Track current = playlist.get(index);
                    updatePlaylist();
                    index = indexOf(current);
            }
            ApexControl.update();
        }
    }

    public void stopCurrentTrack() {
        getPlaylist().get(getIndex()).close();
    }

    public void changeTrack(int change) {
        stopCurrentTrack();
        Track current = playlist.get(index += change);
        current.play();
        trackName.setText(current.getName());
        trackDescription.setText(current.getDescription());
        trackId.setText(current.getId());
    }

    public int indexOf(Track track) {
        List<String> trackNames = playlist.stream().map(Track::getName).collect(Collectors.toList());
        String trackName = track.getName();
        for (int i = 0; i < trackNames.size(); i++) {
            if (Objects.equals(trackNames.get(i), trackName)) {
                return i;
            }
        }
        return -1;
    }

    public static <E> List<E> shuffle(List<E> base) {
        Random random = new Random();
        List<E> copy = new ArrayList<>(base);
        List<E> tor = new ArrayList<>();
        for (int i = copy.size(); i > 0; i--) {
            int index = random.nextInt(i);
            tor.add(copy.remove(index));
        }
        return tor;
    }
}
