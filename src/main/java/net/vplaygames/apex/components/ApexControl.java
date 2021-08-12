package net.vplaygames.apex.components;

import net.vplaygames.apex.Util;

import javax.swing.*;

import static net.vplaygames.apex.Apex.apex;

public class ApexControl {
    public static WrappedTextArea trackId;
    public static WrappedTextArea trackName;
    public static WrappedTextArea trackDescription;
    public static WrappedTextArea trackIndex;
    public static WrappedTextArea fileProgressText;
    public static WrappedTextArea totalProgressText;
    public static WrappedTextArea tracksFound;
    public static SearchTextArea searchTextArea;
    public static JProgressBar fileProgressBar;
    public static JProgressBar totalProgressBar;
    public static JButton next;
    public static JButton previous;
    public static JButton shuffle;
    public static JButton playPause;
    public static JButton stop;
    public static JButton search;
    public static JButton lookupTracks;
    public static JButton downloadAll;
    public static boolean playing = true;
    public static boolean stopped = false;

    public static void init() throws Exception {
        Util.lookAndFeel();
        trackName = Util.makeTextArea("Track Name");
        trackDescription = Util.makeTextArea("Track Description");
        trackId = Util.makeTextArea("Track ID");
        trackIndex = Util.makeTextArea("Index of the track in the playlist");
        searchTextArea = new SearchTextArea("Search and Play");

        next = Util.makeButton("Next Track", "Go to the next track", 0);
        previous = Util.makeButton("Previous Track", "Go to the previous track", 1);
        shuffle = Util.makeButton("Shuffle", "Shuffle the playlist", 2);
        stop = Util.makeButton("Stop", "Stop the track", 3);
        playPause = Util.makeButton("Play", "Play the track", 4);
        search = Util.makeButton("Search and Play", "Type the name of a track above to search and play it" +
            "For example: Typing 'Wally' plays 'Battle! Wally'", 5);

        tracksFound = new WrappedTextArea("0 new tracks found");
        fileProgressText = new WrappedTextArea();
        totalProgressText = new WrappedTextArea();
        fileProgressBar = Util.apply(new JProgressBar(), bar -> bar.setStringPainted(true));
        totalProgressBar = Util.apply(new JProgressBar(), bar -> bar.setStringPainted(true));
    }

    public static void update() {
        int index = apex.getIndex();
        trackIndex.setText("Track " + (index + 1) + "/" + apex.getPlaylist().size());
        next.setEnabled(index != apex.getPlaylist().size() - 1);
        previous.setEnabled(index != 0);
        stop.setEnabled(!stopped);
        playPause.setText(playing ? "Pause" : "Play");
        playPause.setToolTipText(playing ? "Pause the track" : "Play the track");
    }
}
