package net.vpg.apex.core;

import net.vpg.apex.Util;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.stream.Collectors;

public record Track(
    String id,
    String name,
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
            data.getInt("loopStart"),
            data.getInt("loopEnd")
        );
    }

    public Track {
        logger.info("Loaded Track Info for ID: {}", id);
    }

    public static Track of(File file) {
        return entries.get(Util.removeExtension(file.getName()));
    }

    public File getFile() {
        return Resources.get(STR."\{id}.ogg");
    }
}
