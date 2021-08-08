package net.vplaygames.apex;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class AudioPlayerEX {
    List<Track> playlist = new ArrayList<>();
    WrappedTextArea trackId;
    WrappedTextArea trackName;
    WrappedTextArea trackDescription;
    WrappedTextArea trackIndex;
    SearchTextArea searchTextArea;
    JButton next;
    JButton previous;
    JButton shuffle;
    JButton playPause;
    JButton stop;
    JButton search;
    final Object lock = new Object();
    boolean playing = true;
    boolean stopped = false;
    int index = 0;

    public AudioPlayerEX() throws Exception {
        createWindow();
        playlist.addAll(Resources.getResources()
            .values()
            .stream()
            .filter(f -> f.getName().endsWith(".ogg"))
            .map(Track::get)
            .sorted(Comparator.comparing(Track::getId))
            .collect(Collectors.toList()));
        changeTrack(0);
        updateComponents();
    }

    public static void main(String[] args) throws Exception {
        new AudioPlayerEX();
    }

    public List<Track> getPlaylist() {
        return playlist;
    }

    public int getIndex() {
        return index;
    }

    public void takeAction(int action) {
        synchronized (lock) {
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
                    index = indexOf(playlist.stream().map(Track::getName).collect(Collectors.toList()), track.getName());
                    break;
                case 3:
                    stopCurrentTrack();
                    playing = false;
                    stopped = true;
                    break;
                case 4:
                    boolean playing = track.getClip().isActive();
                    if (playing) {
                        track.stop();
                    } else {
                        track.play();
                        stopped = false;
                    }
                    this.playing = !playing;
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
            }
            updateComponents();
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

    public void createWindow() throws Exception {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        }
        JFrame frame = new JFrame("PM APEX");
        initComponents();
        JTabbedPane pane = new JTabbedPane();
        frame.add(pane);
        pane.add(Util.apply(new JPanel(),
            panel -> panel.setName("Control Panel"),
            panel -> panel.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15))),
            panel -> panel.setLayout(new BorderLayout()),
            panel -> panel.add(Util.apply(Box.createVerticalBox(),
                box -> box.add(trackName),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(trackDescription),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(trackDescription),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(trackId),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(trackIndex),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(Util.apply(new JPanel(),
                    buttonPanel -> buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER)),
                    buttonPanel -> buttonPanel.setAlignmentX(0),
                    buttonPanel -> buttonPanel.add(shuffle),
                    buttonPanel -> buttonPanel.add(next),
                    buttonPanel -> buttonPanel.add(stop),
                    buttonPanel -> buttonPanel.add(playPause),
                    buttonPanel -> buttonPanel.add(previous))),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(searchTextArea),
                box -> box.add(Util.apply(new JPanel(),
                    buttonPanel -> buttonPanel.setAlignmentX(0),
                    buttonPanel -> buttonPanel.add(search)))
            ), "North")));
        pane.add(Util.apply(new JPanel(),
            panel -> panel.setName("Credits and Info"),
            panel -> panel.setBorder(new EmptyBorder(new Insets(15, 15, 0, 15))),
            panel -> panel.setLayout(new BorderLayout()),
            panel -> panel.add(Util.apply(Box.createVerticalBox(),
                box -> box.add(Util.apply(new WrappedTextArea("Welcome to Pokemon Masters Audio Player EX, PM APEX in short."), jLabel -> jLabel.setAlignmentX(0))),
                box -> box.add(Box.createVerticalStrut(10)),
                box -> box.add(Util.apply(new WrappedTextArea("This is an application made for playing audio tracks from Pokemon Masters. " +
                    "It also has looping support, which means you can loop your favourite battle theme for as long as you want. " +
                        "It also comes preloaded with all the battle themes. " +
                        "Have Fun!"), jLabel -> jLabel.setAlignmentX(0))),
                box -> box.add(Box.createVerticalStrut(20)),
                box -> box.add(Util.apply(new WrappedTextArea("Credits"), jLabel -> jLabel.setAlignmentX(0), jLabel -> jLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14)))),
                box -> box.add(Box.createVerticalStrut(5)),
                box -> box.add(Util.apply(new WrappedTextArea("V Play Games - The Author and Maintainer of this project"), jLabel -> jLabel.setAlignmentX(0))),
                box -> box.add(Box.createVerticalStrut(3)),
                box -> box.add(Util.apply(new WrappedTextArea("Trilarion (GitHub) - For Providing OGG File Support"), jLabel -> jLabel.setAlignmentX(0))),
                box -> box.add(Box.createVerticalStrut(3)),
                box -> box.add(Util.apply(new WrappedTextArea("Made with Java 8 (Build: 261) using IntelliJ IDE\nBuilt with Maven 3"), jLabel -> jLabel.setAlignmentX(0))),
                box -> box.add(Box.createVerticalStrut(0))
            ), "North")));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setResizable(true);
        frame.setMinimumSize(new Dimension(500, 260));
        frame.setIconImage(ImageIO.read(Resources.geFile("PikaChill.png")));
        frame.pack();
        frame.setVisible(true);
    }

    public void initComponents() {
        trackName = makeTextArea("Track Name");
        trackDescription = makeTextArea("Track Description");
        trackId = makeTextArea("Track ID");
        trackIndex = makeTextArea("Index of the track in the playlist");
        searchTextArea = Util.apply(new SearchTextArea("Search and Play"),
            text -> text.setAlignmentX(0));

        next = makeButton("Next Track", "Go to the next track", 0);
        previous = makeButton("Previous Track", "Go to the previous track", 1);
        shuffle = makeButton("Shuffle", "Shuffle the playlist", 2);
        stop = makeButton("Stop", "Stop the track", 3);
        playPause = makeButton("Play", "Play the track", 4);
        search = makeButton("Search and Play", "Type the name of a track above to search and play it" +
            "For example: Typing 'Wally' plays 'Battle! Wally'", 5);
    }

    private JButton makeButton(String name, String toolTip, int action) {
        return  Util.apply(new JButton(name),
            button -> button.setToolTipText(toolTip),
            button -> button.addActionListener(e -> takeAction(action)));
    }

    private WrappedTextArea makeTextArea(String toolTip) {
        return Util.apply(new WrappedTextArea("Loading..."), text -> text.setToolTipText(toolTip), text -> text.setAlignmentX(0));
    }

    public void updateComponents() {
        trackIndex.setText("Track " + (index + 1) + "/" + playlist.size());
        next.setEnabled(index != playlist.size() - 1);
        previous.setEnabled(index != 0);
        stop.setEnabled(!stopped);
        playPause.setText(playing ? "Pause" : "Play");
        playPause.setToolTipText(playing ? "Pause the track" : "Play the track");
    }

    public <E> int indexOf(List<E> elements, E toFind) {
        for (int i = 0; i < elements.size(); i++) {
            if (Objects.equals(elements.get(i), toFind)) {
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
