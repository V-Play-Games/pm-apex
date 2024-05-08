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

public class TrackMetadata {
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
        int loopStart = -1, loopEnd = -1, loopLength = -1, frameLength;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = headerPattern.matcher(line);
                while (m.find()) {
                    int val = Integer.parseInt(m.group(2));
                    switch (m.group(1)) {
                        case "LOOPSTART":
                            loopStart = val;
                            break;
                        case "LOOPEND":
                            loopEnd = val;
                            break;
                        case "LOOPLENGTH":
                            loopLength = val;
                            break;
                    }
                }
                if (loopStart != -1 && (loopEnd != -1 || loopLength != -1)) {
                    break;
                }
            }
            frameLength = frameLength(file);
            if (loopLength != -1) {
                loopEnd = loopStart + loopLength;
            }
            if (loopEnd > frameLength) {
                loopEnd = frameLength;
            }
        } catch (IOException | UnsupportedAudioFileException e) {
            throw new RuntimeException(e);
        }
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
