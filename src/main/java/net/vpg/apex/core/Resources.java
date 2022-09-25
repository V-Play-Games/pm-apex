package net.vpg.apex.core;

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.apex.components.Downloader;
import net.vpg.vjson.value.JSONArray;
import net.vpg.vjson.value.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.nio.file.StandardWatchEventKinds.*;

public class Resources {
    private static final Logger logger = LoggerFactory.getLogger(Resources.class);
    private static Resources instance;
    private final JSONObject info;
    private final String repo;
    private final String additionalRes;
    private final Path configDir;
    private final Path dataDir;
    private final Path cacheDir;
    private final Map<String, File> resources;

    private Resources() {
        // init basic json info
        info = Util.compute(Apex.class.getResource("info.json"), JSONObject::parse);
        repo = info.getString("repo");
        additionalRes = info.getString("additionalRes");
        String appName = info.getString("appName");

        // init directories
        String os = System.getProperty("os.name");
        String home = System.getProperty("user.home");
        Path[] paths = new Path[3];
        if (os.contains("Mac")) {
            paths[0] = Paths.get(home, "Library", "Application Support");
        } else if (os.contains("Windows")) {
            paths[0] = getPathFromEnv("APPDATA", false, home, "AppData", "Roaming");
            paths[1] = getPathFromEnv("LOCALAPPDATA", false, home, "AppData", "Local");
        } else { // Linux/Unix
            paths[0] = getPathFromEnv("XDG_CONFIG_HOME", true, home, ".config");
            paths[1] = getPathFromEnv("XDG_CACHE_HOME", true, home, ".cache");
            paths[2] = getPathFromEnv("XDG_DATA_HOME", true, home, ".local", "share");
        }
        configDir = paths[0].resolve(appName);
        cacheDir = paths[1] == null ? configDir : paths[1].resolve(appName);
        dataDir = paths[2] == null ? configDir : paths[2].resolve(appName);
        File directory = dataDir.toFile();
        //noinspection ResultOfMethodCallIgnored
        directory.mkdirs();
        resources = Util.collectFilesOf(directory).stream().collect(Collectors.toMap(File::getName, file -> file));
        new Thread(this::watchDataDir, "Directory Watcher").start();
        ifFileExists("info.json", file -> {
            JSONObject json = Util.compute(file, JSONObject::parse);
            if (info.getBoolean("override") || json.getInt("version") < info.getInt("version")) {
                shiftFiles();
            }
        }, this::shiftFiles);
    }

    public static Resources getInstance() {
        return instance == null ? instance = new Resources() : instance;
    }

    public static File get(String filename) {
        return getInstance().resources.get(filename);
    }

    public String getAdditionalRes() {
        return additionalRes;
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
        logger.warn(envVar + " not defined in environment, falling back on \"" + defaultPath + "\"");
        return defaultPath;
    }

    private void watchDataDir() {
        try (WatchService service = dataDir.getFileSystem().newWatchService()) {
            dataDir.register(service, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            for (WatchKey key = service.take(); key.reset(); key = service.take()) {
                key.pollEvents().forEach(event -> {
                    WatchEvent.Kind<?> kind = event.kind();
                    Path path = (Path) event.context();
                    File file = dataDir.resolve(path.getFileName()).toFile();
                    if (kind == ENTRY_CREATE || kind == ENTRY_MODIFY) {
                        resources.put(file.getName(), file);
                    } else { // kind == ENTRY_DELETE
                        resources.remove(file.getName());
                    }
                });
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void shiftFiles() {
        info.getArray("required").stream(JSONArray::getString).forEach(this::shiftFile);
        shiftFile("info.json");
    }

    private void shiftFile(String resource) {
        try (InputStream input = Apex.class.getResource(resource).openStream()) {
            try (OutputStream output = new FileOutputStream(Paths.get(dataDir.toString(), resource).toString())) {
                byte[] buffer = new byte[1024];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
            }
        } catch (IOException e) {
            logger.warn("Unable to copy " + resource + " to the resource directory");
            e.printStackTrace();
        }
    }

    public Path getConfigDir() {
        return configDir;
    }

    public Path getDataDir() {
        return dataDir;
    }

    public Path getCacheDir() {
        return cacheDir;
    }

    public String getBaseDownloadUrl() {
        return "https://raw.githubusercontent.com/" + repo + "/release/";
    }

    public File create(String filename) {
        File file = dataDir.resolve(filename).toFile();
        resources.put(filename, file);
        return file;
    }

    public boolean hasFile(String filename) {
        return resources.containsKey(filename);
    }

    public void ifFileExists(String filename, Consumer<File> ifExists, Runnable ifNot) {
        File file = resources.get(filename);
        if (file != null) {
            ifExists.accept(file);
        } else if (ifNot != null) {
            ifNot.run();
        }
    }

    public Map<String, File> getResources() {
        return resources;
    }

    private List<OnlineTrack> getOnlineResources() throws IOException {
        return JSONArray.parse(Downloader.download("https://api.github.com/repos/" + repo + "/contents/" + additionalRes, "contents.json", null))
            .stream(JSONArray::getObject)
            .map(jo -> new OnlineTrack(jo.getString("name"), jo.getLong("size")))
            .collect(Collectors.toList());
    }

    public List<OnlineTrack> getMissingTracks() {
        List<String> availableIds = Apex.APEX.getPlaylist().stream().map(Track::getId).collect(Collectors.toList());
        return Util.get(this::getOnlineResources).stream()
            .filter(ot -> ot.getName().endsWith(".ogg"))
            .filter(ot -> !availableIds.contains(Util.getId(ot.getName())))
            .collect(Collectors.toList());
    }
}
