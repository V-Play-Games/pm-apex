package net.vpg.apex.components;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class ApexControl {
    public static final WrappedTextArea trackId;
    public static final WrappedTextArea trackName;
    public static final WrappedTextArea trackIndex;
    public static final WrappedTextArea fileProgressText;
    public static final WrappedTextArea totalProgressText;
    public static final WrappedTextArea tracksFound;
    public static final SearchTextArea searchTextArea;
    public static final JProgressBar fileProgressBar;
    public static final JProgressBar totalProgressBar;
    public static final JButton next;
    public static final JButton previous;
    public static final JButton shuffleButton;
    public static final JButton playPause;
    public static final JButton stop;
    public static final JButton search;
    public static final JButton lookupTracks;
    public static final JButton downloadAll;
    public static final DefaultListModel<String> trackListModel;
    public static final JList<String> trackList;
    public static final JScrollPane trackListPane;

    static {
        trackListModel = new DefaultListModel<>();
        trackName = Util.makeTextArea("Track Name", textArea -> textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12)));
        trackId = Util.makeTextArea("Track ID", textArea -> textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)));
        trackIndex = Util.makeTextArea("Index of the track in the playlist");
        searchTextArea = new SearchTextArea("Search and Play");
        searchTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    Apex.APEX.takeAction(5);
                }
            }
        });

        next = Util.makeButton("Next Track", "Go to the next track", 0);
        previous = Util.makeButton("Previous Track", "Go to the previous track", 1);
        shuffleButton = Util.makeButton("Shuffle OFF", "Shuffle the playlist", 2);
        stop = Util.makeButton("Stop", "Stop the track", 3);
        playPause = Util.makeButton("Play", "Play the track", 4);
        search = Util.makeButton("Search and Play", "Search a track", 5);

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
            Util.run(() -> Downloader.download(Resources.getInstance().getBaseDownloadUrl() + "src/main/resources/net/vpg/apex/tracks.json", null));
        });

        trackList = new JList<>(ApexControl.trackListModel);
        trackList.setVisibleRowCount(7);
        trackList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    Apex.APEX.takeAction(7);
                }
            }
        });
        trackList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    Apex.APEX.takeAction(7);
                }
            }
        });
        trackListPane = new JScrollPane(trackList);
    }
}
