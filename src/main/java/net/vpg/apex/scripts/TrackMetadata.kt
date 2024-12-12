import net.vpg.apex.Util.deepListFiles
import net.vpg.apex.core.Resources
import net.vpg.vjson.value.JSONArray
import net.vpg.vjson.value.JSONObject
import java.io.File
import javax.sound.sampled.AudioSystem

fun main() {
    val array = File("D:/Projects/Apex").deepListFiles()
        .filter { it.getName().endsWith(".ogg") }
        .map {
            JSONObject()
                .put("name", it.getName().replace(".ogg", ""))
                .put("id", it.getName().replace(".ogg", ""))
                .put("category", it.getParentFile().getName())
        }
        .stream()
        .collect(JSONArray.collector())
        .toList()

    val entries = JSONArray.parse(Resources["tracks.json"]).toList()
    val collect = entries
        .map { it.toObject() }
        .associate { Pair(it.getString("id"), it) }
    array.map { it.toObject() }
        .filter { !collect.containsKey(it.getString("id")) }
        .forEach { entries.add(it) }
    entries.map { it.toObject() }
        .sortedBy { it.getString("id") }
        .distinct()
        .onEach { init(it) }
        .map { it.toString() }
        .forEach { println(it) }
}

fun init(obj: JSONObject) {
    if (!obj.isNull("frameLength")) {
        println("already read")
        return
    }
    val file = File("D:/Projects/Apex/${obj.getString("category")}/${obj.getString("id")}.ogg")
    if (!file.exists()) {
        println("$file doesn't exist, skipping...")
        return
    }
    try {
        AudioSystem.getAudioInputStream(file).use { stream ->
            val aff = AudioSystem.getAudioFileFormat(file)
            val loopStart = aff.getProperty("LOOPSTART").toProperInt()
            var loopEnd = aff.getProperty("LOOPEND").toProperInt()
            val loopLength = aff.getProperty("LOOPLENGTH").toProperInt()
            val frameLength = stream.readAllBytes().size / stream.format.frameSize
            if (loopLength != -1) loopEnd = loopStart + loopLength
            if (loopEnd == -1 || loopEnd > frameLength) loopEnd = frameLength
            obj.put("loopStart", loopStart)
                .put("loopEnd", loopEnd)
                .put("frameLength", frameLength)
        }
    } catch (e: Exception) {
        println(obj.getString("id") + " ERROR")
        e.printStackTrace()
    }
}

private fun Any?.toProperInt() = this?.toString()?.trim()?.toInt() ?: -1
