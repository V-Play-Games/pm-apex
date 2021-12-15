package net.vplaygames.apex.core;

import net.vplaygames.apex.Util;
import net.vplaygames.vjson.value.JSONArray;
import net.vplaygames.vjson.value.JSONObject;
import net.vplaygames.vjson.value.JSONValue;

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
    public static final Map<String, TrackInfo> entries =
        Util.compute(Resources.getFile("tracks.json"), JSONObject::parse)
            .getArray("entries")
            .stream(JSONArray::getObject)
            .collect(Collectors.toMap(jo -> jo.getString("id"), TrackInfo::new));

    static final JSONValue notAvailable = JSONValue.of("N/A");
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
        String fileName = data.getString("id") + ".ogg";
        if (Resources.hasFile(fileName)) {
            init(Resources.getFile(fileName));
        }
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
        JSONObject obj = new JSONObject();
        obj.put("id", JSONValue.of(id));
        obj.put("name", notAvailable);
        obj.put("description", notAvailable);
        return new TrackInfo(obj);
    }

    private void init(File file) {
        this.file = file;
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
        id = jo.getString("id");
        name = jo.getString("name");
        description = jo.getString("description");
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
