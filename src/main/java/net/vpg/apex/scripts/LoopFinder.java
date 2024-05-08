package net.vpg.apex.scripts;

import net.vpg.apex.core.Resources;
import net.vpg.vjson.value.JSONObject;
import net.vpg.vjson.value.JSONValue;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoopFinder {
    public static final Pattern headerPattern = Pattern.compile("([A-Z]+)=(\\d+)");

    public static void main() throws FileNotFoundException {
        JSONObject.parse(Resources.get("tracks.json"))
            .getArray("entries")
            .stream()
            .map(JSONValue::toObject)
            .peek(obj -> init(new File(STR."bgm/\{obj.getString("id")}.ogg"), obj))
            .map(JSONValue::toString)
            .forEach(System.out::println);
    }

    private static void init(File file, JSONObject obj) {
        if (!file.exists())
            System.out.println(file + " doesn't exist, skipping...");
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
            obj.put("frameLength", frameLength(file));
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
    }

    public static long frameLength(File file) throws IOException, UnsupportedAudioFileException {
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
        long frameLength = stream.getFrameLength();
        if (frameLength != AudioSystem.NOT_SPECIFIED) {
            return frameLength;
        }
        int frameSize = stream.getFormat().getFrameSize();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[512 * frameSize];
        int read;
        while ((read = stream.read(buffer)) != -1)
            outputStream.write(buffer, 0, read);
        return outputStream.size() / frameSize;
    }
}
