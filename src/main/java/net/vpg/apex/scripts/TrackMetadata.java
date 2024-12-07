/*
 * Copyright 2021 Vaibhav Nargwani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;
import net.vpg.vjson.value.JSONArray;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.File;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

void main() {
    JSONArray array = Util.collectFilesOf(new File("D:/Projects/Apex"))
        .stream()
        .filter(file -> file.getName().endsWith(".ogg"))
        .map(file -> new JSONObject()
            .put("name", file.getName().replace(".ogg", ""))
            .put("id", file.getName().replace(".ogg", ""))
            .put("category", file.getParentFile().getName()))
        .collect(JSONArray.collector());

    JSONArray entries = Resources.get("tracks.json", JSONArray::parse);
    Map<String, JSONObject> collect = entries.stream()
        .map(JSONValue::toObject)
        .collect(Collectors.toMap(obj -> obj.getString("id"), obj -> obj));
    array.stream()
        .map(JSONValue::toObject)
        .filter(obj -> !collect.containsKey(obj.getString("id")))
        .forEach(entries::add);
    entries.stream()
        .map(JSONValue::toObject)
        .sorted(Comparator.comparing(obj -> obj.getString("id")))
        .distinct()
        .peek(this::init)
        .map(JSONValue::toString)
        .forEach(System.out::println);
}

void init(JSONObject obj) {
    if (!obj.isNull("frameLength")) {
        System.out.println("already read");
        return;
    }
    File file = new File("D:/Projects/Apex/" + obj.getString("category") + "/" + obj.getString("id") + ".ogg");
    if (!file.exists()) {
        System.out.println(file + " doesn't exist, skipping...");
        return;
    }
    try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
        AudioFileFormat aff = AudioSystem.getAudioFileFormat(file);
        int loopStart = getInt(aff.getProperty("LOOPSTART"));
        int loopEnd = getInt(aff.getProperty("LOOPEND"));
        int loopLength = getInt(aff.getProperty("LOOPLENGTH"));
        int frameLength = stream.readAllBytes().length / stream.getFormat().getFrameSize();
        if (loopLength != -1)
            loopEnd = loopStart + loopLength;
        if (loopEnd == -1 || loopEnd > frameLength)
            loopEnd = frameLength;
        obj.put("loopStart", loopStart)
            .put("loopEnd", loopEnd)
            .put("frameLength", frameLength);
    } catch (Exception e) {
        System.out.println(obj.getString("id") + " ERROR");
        e.printStackTrace();
    }
}

private int getInt(Object object) {
    return object == null ? -1 : Integer.parseInt(object.toString().trim());
}
