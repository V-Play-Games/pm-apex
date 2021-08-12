package net.vplaygames.apex.core;

import net.vplaygames.apex.Util;
import net.vplaygames.vjson.JSONObject;
import net.vplaygames.vjson.JSONValue;

import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TrackInfo {
    public static final Pattern loopStartPattern = Pattern.compile("LOOPSTART=(\\d+)");
    public static final Pattern loopEndPattern = Pattern.compile("LOOPEND=(\\d+)");
    public static final Map<String, TrackInfo> entries = Util.get(() -> JSONValue.parse(Resources.geFile("tracks.json"))
        .asObject().get("entries").asArray()
        .stream()
        .map(JSONValue::asObject)
        .collect(Collectors.toMap(jo -> jo.get("id").asString(), TrackInfo::new)));

    private JSONObject jo;
    private File file;
    private String id;
    private String name;
    private String description;
    private boolean initDone = false;
    private int loopStart = -1;
    private int loopEnd = -1;

    private TrackInfo(JSONObject data) {
        jo = data;
        String fileName = data.get("id").asString() + ".ogg";
        if (Resources.hasFile(fileName)) {
            init(fileName);
        }
    }

    private void init(String fileName) {
        file = Resources.geFile(fileName);
        System.out.println("Loaded " + file.getName());
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
        id = jo.get("id").asString();
        name = jo.get("name").asString();
        description = jo.get("description").asString();
        initDone = true;
    }

    private boolean isInitDone() {
        return initDone;
    }

    public static TrackInfo get(File file) {
        String filename = file.getName();
        TrackInfo info = entries.get(Util.getId(filename));
        if (!info.isInitDone()) {
            info.init(filename);
        }
        return info;
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
