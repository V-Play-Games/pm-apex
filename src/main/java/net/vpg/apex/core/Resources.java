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

import net.vpg.apex.Apex;
import net.vpg.apex.Util;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class Resources {
    private static final Logger logger = LoggerFactory.getLogger(Resources.class);
    private static final Resources instance = new Resources();
    private final JSONObject properties;
    private final File dataDir;
    private final Map<String, File> resources;

    private Resources() {
        // init basic json info
        properties = Util.compute(Apex.class.getResource("info.json"), JSONObject::parse);

        // init directories
        String os = System.getProperty("os.name");
        String home = System.getProperty("user.home");
        Path dataPath;
        if (os.contains("Mac"))
            dataPath = Paths.get(home, "Library", "Application Support");
        else if (os.contains("Windows"))
            dataPath = getPathFromEnv("LOCALAPPDATA", false, home, "AppData", "Local");
        else // Linux/Unix
            dataPath = getPathFromEnv("XDG_DATA_HOME", true, home, ".local", "share");
        dataDir = dataPath.resolve(properties.getString("appName")).toFile();
        //noinspection ResultOfMethodCallIgnored
        dataDir.mkdirs();
        resources = Util.collectFilesOf(dataDir)
            .stream()
            .collect(Collectors.toMap(File::getName, file -> file));
        properties.getArray("required")
            .stream()
            .map(JSONValue::toString)
            .forEach(this::shiftFile);
    }

    public static File get(String filename) {
        return instance.resources.get(filename);
    }

    public static <T> T get(String filename, Util.FunctionWithAChanceOfException<File, T> func) {
        return Util.compute(get(filename), func);
    }

    public static JSONValue getProperty(String prop) {
        return instance.properties.get(prop);
    }

    public static File create(String filename) {
        File file = new File(instance.dataDir, filename);
        instance.resources.put(filename, file);
        return file;
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
        try (InputStream input = Apex.class.getResourceAsStream(resource);
             FileOutputStream output = new FileOutputStream(new File(dataDir, resource))) {
            //noinspection DataFlowIssue
            input.transferTo(output);
        } catch (IOException e) {
            logger.error(STR."Unable to copy \{resource} to the resource directory", e);
        }
    }
}
