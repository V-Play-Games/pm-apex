package net.vpg.apex.core;

import net.vpg.apex.Util;
import net.vpg.vjson.value.JSONArray;
import net.vpg.vjson.value.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrackInfo {
    public static final Pattern loopStartPattern = Pattern.compile("LOOPSTART=(\\d+)");
    public static final Pattern loopEndPattern = Pattern.compile("LOOPEND=(\\d+)");
    private static final Logger logger = LoggerFactory.getLogger(TrackInfo.class);
    public static final Map<String, TrackInfo> entries =
        Util.compute(Resources.get("tracks.json"), JSONObject::parse)
            .getArray("entries")
            .stream(JSONArray::getObject)
            .map(TrackInfo::new)
            .collect(Collectors.toMap(TrackInfo::getId, info -> info));
    private final String id;
    private final String name;
    private final String description;
    private File file;
    private boolean initDone = false;
    private int loopStart = -1;
    private int loopEnd = -1;

    private TrackInfo(JSONObject data) {
        id = data.getString("id");
        name = data.getString("name");
        description = data.getString("description");
        logger.info("Loaded Track Info for ID: " + id);
        String fileName = id + ".ogg";
        Resources.getInstance().ifFileExists(fileName, this::init, null);
    }

    public static TrackInfo get(File file) {
        String filename = file.getName();
        TrackInfo info = entries.computeIfAbsent(Util.getId(filename), TrackInfo::getInfo);
        if (!info.isInitDone()) {
            info.init(file);
        }
        return info;
    }

    private static TrackInfo getInfo(String id) {
        return new TrackInfo(new JSONObject()
            .put("id", id)
            .put("name", "N/A")
            .put("description", "N/A"));
    }

    private void init(File file) {
        this.file = file;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (loopStart == -1) {
                    Matcher m = loopStartPattern.matcher(line);
                    if (m.find())
                        loopStart = Integer.parseInt(m.group(1));
                }
                if (loopEnd == -1) {
                    Matcher m = loopEndPattern.matcher(line);
                    if (m.find())
                        loopEnd = Integer.parseInt(m.group(1));
                }
                if (loopStart != -1 && loopEnd != -1) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        initDone = true;
    }

    private boolean isInitDone() {
        return initDone;
    }

    public File getFile() {
        return file;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getLoopStart() {
        return loopStart;
    }

    public int getLoopEnd() {
        return loopEnd;
    }
}
