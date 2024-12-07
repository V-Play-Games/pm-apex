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
package net.vpg.apex.core

import net.vpg.apex.Apex
import java.io.File
import java.net.URL
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem

class AudioData(private val stream: AudioInputStream, val frameLength: Int) {
    private val data = ByteArray(frameLength * format.frameSize)
    var readPos = 0
    private var cachedPos = 0
    private var caching = false

    constructor(file: File, frameLength: Int) : this(AudioSystem.getAudioInputStream(file), frameLength)

    constructor(url: URL, frameLength: Int) : this(AudioSystem.getAudioInputStream(url), frameLength)

    val format
        get() = stream.format

    fun startCaching() {
        if (caching) return
        caching = true
        Apex.EXECUTOR.execute { cache() }
    }

    fun readData(frames: Int): ByteArray {
        if (readPos + frames <= cachedPos) {
            readPos += frames
            return data.copyOfRange(readPos * format.frameSize, (readPos + frames) * format.frameSize)
        }
        if (caching) {
            Thread.sleep(25)
            return readData(frames)
        }
        throw IllegalStateException()
    }

    fun cache() {
        var off = 0
        while (caching && (stream.read(data, off, data.size - off).also { off += it }) != -1) {
            cachedPos = off / format.frameSize
        }
        close()
    }

    fun close() {
        caching = false
        stream.close()
    }
}
