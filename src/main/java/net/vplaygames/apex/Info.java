package net.vplaygames.apex;

import net.vplaygames.vjson.JSONObject;
import net.vplaygames.vjson.JSONValue;

import java.io.*;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Info {
    public static final Pattern loopStartPattern = Pattern.compile("LOOPSTART=(\\d+)");
    public static final Pattern loopEndPattern = Pattern.compile("LOOPEND=(\\d+)");
    public static final Map<String, Info> entries = Util.get(() -> JSONValue.parse(Resources.geFile("tracks.json"))
        .asObject().get("entries").asArray()
        .stream()
        .map(JSONValue::asObject)
        .collect(Collectors.toMap(jo -> jo.get("id").asString(), Info::new)));

    private File file;
    private String id;
    private String name;
    private String description;
    private int loopStart = -1;
    private int loopEnd = -1;

    private Info(JSONObject data) {
        String fileName = data.get("id").asString() + ".ogg";
        if (Resources.hasFile(fileName)) {
            init(fileName, data);
        }
    }

    private void init(String fileName, JSONObject data) {
        file = Resources.geFile(fileName);
        System.out.println(file.getName());
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
        id = data.get("id").asString();
        name = data.get("name").asString();
        description = data.get("description").asString();
    }

    public static Info get(File file) {
        return entries.get(Util.getId(file.getName()));
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
