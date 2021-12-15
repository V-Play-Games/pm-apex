package net.vpg.apex.core;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.apex.components.Downloader;
import net.vpg.vjson.value.JSONArray;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Resources {
    public static final String baseDownloadUrl = "https://raw.githubusercontent.com/V-Play-Games/pm-apex/release/resources/";
    private static final File resourceDirectory = new File("resources");
    private static final Map<String, File> resources = Util.collectFilesOf(resourceDirectory).stream().collect(Collectors.toMap(File::getName, UnaryOperator.identity()));

    public static File makeFile(String filename) {
        File file = new File(resourceDirectory, filename);
        resources.put(filename, file);
        return file;
    }

    public static File getFile(String filename) {
        return resources.get(filename);
    }

    public static boolean hasFile(String filename) {
        return resources.containsKey(filename);
    }

    public static Map<String, File> getResources() {
        return resources;
    }

    private static List<OnlineTrack> getOnlineResources() throws IOException {
        return JSONArray.parse(Downloader.download("https://api.github.com/repos/V-Play-Games/pm-apex/contents/resources", "contents.json", null))
            .stream(JSONArray::getObject)
            .map(jo -> new OnlineTrack(jo.getString("name"), jo.getLong("size")))
            .collect(Collectors.toList());
    }

    public static List<OnlineTrack> getMissingTracks() {
        List<String> availableIds = Apex.APEX.getPlaylist().stream().map(Track::getId).collect(Collectors.toList());
        return Util.get(Resources::getOnlineResources).stream()
            .filter(ot -> ot.getName().endsWith(".ogg"))
            .filter(ot -> !availableIds.contains(Util.getId(ot.getName())))
            .collect(Collectors.toList());
    }
}
