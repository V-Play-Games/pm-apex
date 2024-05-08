package net.vpg.apex.components;

import net.vpg.apex.Apex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static net.vpg.apex.Apex.APEX;

public class ApexControl {
    public static final WrappedTextArea trackId;
    public static final WrappedTextArea trackName;
    public static final WrappedTextArea trackIndex;
    public static final SearchTextArea searchTextArea;
    public static final JButton next;
    public static final JButton previous;
    public static final JButton shuffleButton;
    public static final JButton playPause;
    public static final JButton stop;
    public static final JButton search;
    public static final DefaultListModel<String> trackListModel;
    public static final JList<String> trackList;
    public static final JScrollPane trackListPane;

    static {
        trackListModel = new DefaultListModel<>();
        trackName = makeTextArea("Track Name", textArea -> textArea.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12)));
        trackId = makeTextArea("Track ID", textArea -> textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12)));
        trackIndex = makeTextArea("Index of the track in the playlist");
        searchTextArea = new SearchTextArea("Search and Play");
        searchTextArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == '\n') {
                    Apex.APEX.takeAction(5);
                }
            }
        });

        next = makeButton("Next Track", "Go to the next track", 0);
        previous = makeButton("Previous Track", "Go to the previous track", 1);
        shuffleButton = makeButton("Shuffle OFF", "Shuffle the playlist", 2);
        stop = makeButton("Stop", "Stop the track", 3);
        playPause = makeButton("Play", "Play the track", 4);
        search = makeButton("Search and Play", "Search a track", 5);

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

    private static JButton makeButton(String name, String toolTip, int action) {
        JButton button = new JButton(name);
        button.setToolTipText(toolTip);
        button.addActionListener(_ -> APEX.takeAction(action));
        return button;
    }

    @SafeVarargs
    private static WrappedTextArea makeTextArea(String toolTip, Consumer<WrappedTextArea>... actions) {
        WrappedTextArea textArea = new WrappedTextArea();
        for (var action : actions)
            action.accept(textArea);
        textArea.setToolTipText(toolTip);
        return textArea;
    }
}
