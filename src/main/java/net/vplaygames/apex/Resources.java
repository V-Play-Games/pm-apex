package net.vplaygames.apex;

import java.io.File;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Resources {
    private static final Map<String, File> resources = Util.collectFilesOf("resources")
        .stream()
        .collect(Collectors.toMap(File::getName, UnaryOperator.identity()));

    public static File geFile(String filename) {
        return resources.get(filename);
    }

    public static boolean hasFile(String filename) {
        return resources.containsKey(filename);
    }

    public static Map<String, File> getResources() {
        return resources;
    }
}
