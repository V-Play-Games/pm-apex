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

package net.vpg.apex.core;

import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;

public record ApexTrack(
    String id,
    String name,
    String category,
    int frameLength,
    int loopStart,
    int loopEnd
) {
    private static final Logger logger = LoggerFactory.getLogger(ApexTrack.class);
    public static final Map<String, ApexTrack> entries =
        Resources.get("tracks.json", JSONObject::parse)
            .getArray("entries")
            .stream()
            .map(JSONValue::toObject)
            .map(ApexTrack::new)
            .collect(Collectors.toMap(ApexTrack::id, info -> info));

    public ApexTrack(JSONObject data) {
        this(
            data.getString("id"),
            data.getString("name"),
            data.getString("category"),
            data.getInt("frameLength"),
            data.getInt("loopStart"),
            data.getInt("loopEnd")
        );
    }

    public ApexTrack {
        logger.info("Loaded Track Info for ID: {}", id);
    }

    public AudioData getData() throws UnsupportedAudioFileException, IOException {
        File file = getFile();
        if (file != null && file.exists())
            return new AudioData(file, frameLength);
        else
            return new AudioData(getURL(), frameLength);
    }

    public File getFile() {
        return Resources.get(STR."\{id}.ogg");
    }

    public URL getURL() throws MalformedURLException {
        return URI.create(Resources.getProperty("additionalRes")
            .toString()
            .formatted(category, id, "ogg")
            .replace(" ", "%20")).toURL();
    }
}
