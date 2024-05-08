package net.vpg.apex.core;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class Resources {
    private static final Logger logger = LoggerFactory.getLogger(Resources.class);
    private static Resources instance;
    private final URI additionalRes;
    private final File dataDir;
    private final Map<String, File> resources;

    private Resources() {
        // init basic json info
        JSONObject info = Util.compute(Apex.class.getResource("info.json"), JSONObject::parse);
        additionalRes = URI.create(info.getString("additionalRes"));
        String appName = info.getString("appName");

        // init directories
        String os = System.getProperty("os.name");
        String home = System.getProperty("user.home");
        Path dataPath;
        if (os.contains("Mac")) {
            dataPath = Paths.get(home, "Library", "Application Support");
        } else if (os.contains("Windows")) {
            dataPath = getPathFromEnv("LOCALAPPDATA", false, home, "AppData", "Local");
        } else { // Linux/Unix
            dataPath = getPathFromEnv("XDG_DATA_HOME", true, home, ".local", "share");
        }
        dataDir = dataPath.resolve(appName).toFile();
        //noinspection ResultOfMethodCallIgnored
        dataDir.mkdirs();
        resources = Util.collectFilesOf(dataDir).stream().collect(Collectors.toMap(File::getName, file -> file));
        info.getArray("required")
            .stream()
            .map(JSONValue::toString)
            .forEach(this::shiftFile);
    }

    public static Resources getInstance() {
        return instance == null ? instance = new Resources() : instance;
    }

    public static File get(String filename) {
        return getInstance().resources.get(filename);
    }

    private Path getPathFromEnv(String envVar, boolean mustBeAbsolute, String first, String... more) {
        String envDir = System.getenv(envVar);
        if (envDir != null && !envDir.isEmpty()) {
            Path dir = Paths.get(envDir);
            if (!mustBeAbsolute || dir.isAbsolute()) {
                return dir;
            }
        }
        Path defaultPath = Paths.get(first, more);
        logger.warn("{} not defined in environment, falling back on \"{}\"", envVar, defaultPath);
        return defaultPath;
    }

    private void shiftFile(String resource) {
        //noinspection DataFlowIssue
        try (InputStream input = Apex.class.getResource(resource).openStream()) {
            Files.copy(input, dataDir.toPath().resolve(resource));
        } catch (IOException e) {
            logger.error(STR."Unable to copy \{resource} to the resource directory", e);
        }
    }

    public URL getAdditionResourceURL(String res) throws MalformedURLException {
        return additionalRes.resolve(res).toURL();
    }

    public File create(String filename) {
        File file = new File(dataDir, filename);
        resources.put(filename, file);
        return file;
    }
}
