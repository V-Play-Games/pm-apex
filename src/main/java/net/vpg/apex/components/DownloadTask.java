package net.vpg.apex.components;

import net.vpg.apex.Util;
import net.vpg.apex.core.OnlineTrack;
import net.vpg.apex.core.Resources;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import static net.vpg.apex.Apex.LOGGER;

public class DownloadTask implements Downloader.EventListener {
    public static ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2);
    private final long totalSize;
    private final List<OnlineTrack> tracks;
    private final Runnable onEachFileDownloaded;
    private int downloaded;
    private int index;

    public DownloadTask(List<OnlineTrack> tracks, Runnable onEachFileDownloaded) {
        this.tracks = tracks;
        this.onEachFileDownloaded = onEachFileDownloaded;
        this.downloaded = 0;
        this.index = -1;
        this.totalSize = tracks.stream().mapToLong(OnlineTrack::getSize).sum();
        DownloadPanel.getInstance().showDownload();
        downloadNext();
    }

    public void downloadNext() {
        executor.execute(() -> {
            index++;
            if (index == tracks.size()) {
                DownloadPanel.getInstance().hideDownload();
                ApexControl.lookupTracks.setEnabled(true);
                ApexControl.downloadAll.setEnabled(true);
                onEachFileDownloaded.run();
                return;
            }
            OnlineTrack track = tracks.get(index);
            try {
                Resources res = Resources.getInstance();
                Downloader.download(res.getBaseDownloadUrl() + res.getAdditionalRes() + "/" + track.name, this);
            } catch (IOException e) {
                LOGGER.error("Encountered an unexpected uncaught exception:", e);
            }
            onEachFileDownloaded.run();
        });
    }

    @Override
    public void progress(Downloader.Event e) {
        OnlineTrack track = tracks.get(index);
        downloaded += e.bytesRead;
        int fileProgress = (int) (e.totalBytesRead * 100 / track.size);
        int totalProgress = (int) (downloaded * 100 / totalSize);
        ApexControl.fileProgressText.setText("Downloading " + track.name + " | " + Util.bytesToString(e.totalBytesRead) + "/" + Util.bytesToString(track.size));
        ApexControl.totalProgressText.setText("Total Progress: " + Util.bytesToString(downloaded) + "/" + Util.bytesToString(totalSize));
        ApexControl.fileProgressBar.setValue(fileProgress);
        ApexControl.fileProgressBar.setString(fileProgress + "%");
        ApexControl.totalProgressBar.setValue(totalProgress);
        ApexControl.totalProgressBar.setString(totalProgress + "%");
        if (e.type == Downloader.DONE) {
            downloadNext();
        }
    }
}
