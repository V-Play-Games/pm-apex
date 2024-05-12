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

package net.vpg.apex.scripts;

import net.vpg.apex.Util;
import net.vpg.apex.core.Resources;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrackMetadata {
    public static final Pattern headerPattern = Pattern.compile("([A-Z]+)=(\\d+)");

    public static void main() {
        Resources.get("tracks.json", JSONObject::parse)
            .getArray("entries")
            .stream()
            .map(JSONValue::toObject)
            .sorted(Comparator.comparing(obj -> obj.getString("id")))
            .peek(obj -> Util.run(() -> init(new File(STR."bgm/\{obj.getString("id")}.ogg"), obj)))
            .map(JSONValue::toString)
            .forEach(System.out::println);
    }

    private static void init(File file, JSONObject obj) throws UnsupportedAudioFileException, IOException {
        if (!file.exists())
            System.out.println(file + " doesn't exist, skipping...");
        if (!obj.isNull("loopStart") && !obj.isNull("loopEnd") && !obj.isNull("frameLength"))
            return;
        int loopStart = -1, loopEnd = -1, loopLength = -1, frameLength;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            frameLength = frameLength(file);
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = headerPattern.matcher(line);
                while (m.find()) {
                    int val = Integer.parseInt(m.group(2));
                    switch (m.group(1)) {
                        case "LOOPSTART" -> loopStart = val;
                        case "LOOPEND" -> loopEnd = val;
                        case "LOOPLENGTH" -> loopLength = val;
                    }
                }
                if (loopStart != -1 && (loopEnd != -1 || loopLength != -1))
                    break;
            }
        }
        if (loopLength != -1)
            loopEnd = loopStart + loopLength;
        if (loopEnd > frameLength)
            loopEnd = frameLength;
        obj.put("loopStart", loopStart)
            .put("loopEnd", loopEnd)
            .put("frameLength", frameLength);
    }

    public static int frameLength(File file) throws IOException, UnsupportedAudioFileException {
        AudioInputStream sourceStream = AudioSystem.getAudioInputStream(file);
        AudioFormat sourceFormat = sourceStream.getFormat();
        AudioFormat targetFormat = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sourceFormat.getSampleRate(),
            16,
            sourceFormat.getChannels(),
            4,
            sourceFormat.getSampleRate(), // Note: Keep Sample Rate = Frame Rate
            sourceFormat.isBigEndian()
        );
        AudioInputStream stream = AudioSystem.getAudioInputStream(targetFormat, sourceStream);
        int frameLength = (int) stream.getFrameLength();
        return frameLength != AudioSystem.NOT_SPECIFIED
            ? frameLength
            : stream.readAllBytes().length / stream.getFormat().getFrameSize();
    }
}
