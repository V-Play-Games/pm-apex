package net.vpg.apex.components;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.swing.*;
import java.awt.*;

import static net.vpg.apex.Apex.APEX;

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
    public static JButton surpriseMe;
    public static DefaultListModel<String> trackListModel;
    public static JList<String> trackList;
    public static JScrollPane trackListPane;
    public static boolean playing = true;
    public static boolean stopped = false;

    public static void init() throws Exception {
        Util.lookAndFeel();
        trackListModel = new DefaultListModel<>();
        trackName = Util.makeTextArea("Track Name", textArea -> textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12)));
        trackDescription = Util.makeTextArea("Track Description");
        trackId = Util.makeTextArea("Track ID", textArea -> textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)));
        trackIndex = Util.makeTextArea("Index of the track in the playlist");
        searchTextArea = new SearchTextArea("Search and Play");

        next = Util.makeButton("Next Track", "Go to the next track", 0);
        previous = Util.makeButton("Previous Track", "Go to the previous track", 1);
        shuffle = Util.makeButton("Shuffle", "Shuffle the playlist", 2);
        stop = Util.makeButton("Stop", "Stop the track", 3);
        playPause = Util.makeButton("Play", "Play the track", 4);
        search = Util.makeButton("Search and Play", "Type the name of a track above to search and play it" +
            "For example: Typing 'Wally' plays 'Battle! Wally'", 5);
        surpriseMe = Util.makeButton("Surprise Me!", "Plays a random track", 8);

        tracksFound = new WrappedTextArea("0 new tracks found");
        fileProgressText = new WrappedTextArea();
        totalProgressText = new WrappedTextArea();
        fileProgressBar = Util.apply(new JProgressBar(), bar -> bar.setStringPainted(true));
        totalProgressBar = Util.apply(new JProgressBar(), bar -> bar.setStringPainted(true));

        lookupTracks = new JButton("Refresh");
        downloadAll = new JButton("Download all found tracks");
        lookupTracks.addActionListener(e -> tracksFound.setText(Resources.getInstance().getMissingTracks().size() + " more tracks found"));
        downloadAll.addActionListener(e -> {
            ApexControl.lookupTracks.setEnabled(false);
            ApexControl.downloadAll.setEnabled(false);
            new DownloadTask(Resources.getInstance().getMissingTracks(), () -> Apex.APEX.takeAction(6));
        });

    }

    public static void update() {
        int index = APEX.getIndex();
        trackIndex.setText("Track " + (index + 1) + "/" + APEX.getPlaylist().size());
        next.setEnabled(index != APEX.getPlaylist().size() - 1);
        previous.setEnabled(index != 0);
        stop.setEnabled(!stopped);
        playPause.setText(playing ? "Pause" : "Play");
        playPause.setToolTipText(playing ? "Pause the track" : "Play the track");
    }
}
