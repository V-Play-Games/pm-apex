package net.vpg.apex.core;

import net.vpg.apex.Util;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

public record Track(
    String id,
    String name,
    int frameLength,
    int loopStart,
    int loopEnd
) {
    private static final Logger logger = LoggerFactory.getLogger(Track.class);
    public static final Map<String, Track> entries =
        Util.compute(Resources.get("tracks.json"), JSONObject::parse)
            .getArray("entries")
            .stream()
            .map(JSONValue::toObject)
            .map(Track::new)
            .collect(Collectors.toMap(Track::id, info -> info));

    public Track(JSONObject data) {
        this(
            data.getString("id"),
            data.getString("name"),
            data.getInt("frameLength"),
            data.getInt("loopStart"),
            data.getInt("loopEnd")
        );
    }

    public Track {
        logger.info("Loaded Track Info for ID: {}", id);
    }

    public AudioData getData() throws UnsupportedAudioFileException, IOException {
        String filename = STR."\{id}.ogg";
        File file = Resources.get(filename);
        if (file != null && file.exists())
            return new AudioData(getFile(), frameLength);
        else
            return new AudioData(Resources.getInstance().getAdditionResourceURL(filename), frameLength);
    }

    public File getFile() {
        return Resources.get(STR."\{id}.ogg");
    }
}
