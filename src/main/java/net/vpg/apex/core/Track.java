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

public class Track {
    public static final Pattern loopStartPattern = Pattern.compile("LOOPSTART=(\\d+)");
    public static final Pattern loopEndPattern = Pattern.compile("LOOPEND=(\\d+)");
    private static final Logger logger = LoggerFactory.getLogger(Track.class);
    public static final Map<String, Track> entries =
        Util.compute(Resources.get("tracks.json"), JSONObject::parse)
            .getArray("entries")
            .stream(JSONArray::getObject)
            .map(Track::new)
            .collect(Collectors.toMap(Track::getId, info -> info));
    private final String id;
    private final String name;
    private final String description;
    private File file;
    private boolean initDone = false;
    private int loopStart = -1;
    private int loopEnd = -1;

    private Track(JSONObject data) {
        id = data.getString("id");
        name = data.getString("name");
        description = data.getString("description");
        logger.info("Loaded Track Info for ID: " + id);
    }

    public static Track get(File file) {
        String filename = file.getName();
        Track info = entries.computeIfAbsent(Util.getId(filename), Track::getInfo);
        if (!info.isInitDone()) {
            info.init(file);
        }
        return info;
    }

    private static Track getInfo(String id) {
        return new Track(new JSONObject()
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
