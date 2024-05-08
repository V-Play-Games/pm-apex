package net.vpg.apex.scripts;

import net.vpg.apex.core.Resources;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoopFinder {
    public final Pattern headerPattern = Pattern.compile("([A-Z]+)=(\\d+)");

    public void main() throws FileNotFoundException {
        JSONObject.parse(Resources.get("tracks.json"))
            .getArray("entries")
            .stream()
            .map(JSONValue::toObject)
            .peek(obj -> init(new File(STR."bgm/\{obj.getString("id")}.ogg"), obj))
            .map(JSONValue::toString)
            .forEach(System.out::println);
    }

    private void init(File file, JSONObject obj) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = headerPattern.matcher(line);
                while (m.find()) {
                    int val = Integer.parseInt(m.group(2));
                    switch (m.group(1)) {
                        case "LOOPSTART":
                            obj.put("loopStart", val);
                            break;
                        case "LOOPEND":
                            obj.put("loopEnd", val);
                            break;
                    }
                }
                if (!obj.isNull("loopStart") && !obj.isNull("loopEnd")) {
                    break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
