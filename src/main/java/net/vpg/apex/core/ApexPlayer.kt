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
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.SourceDataLine
import kotlin.math.min

class ApexPlayer {
    private var sourceDataLine: SourceDataLine? = null
    private var frameRate = 0
    @Volatile
    private var data: AudioData? = null
    private var loopStart = 0
    private var loopEnd = 0
    var loopCount = 0
    var isPlaying = false
        private set

    fun play(track: ApexTrack) {
        isPlaying = false
        if (data != null) data!!.close()
        data = track.data
        data!!.startCaching()
        val format = data!!.format
        if (frameRate.toFloat() != format.frameRate) {
            frameRate = format.frameRate.toInt()
            if (sourceDataLine != null) {
                sourceDataLine!!.close()
                sourceDataLine!!.open(format)
            } else {
                sourceDataLine = AudioSystem.getSourceDataLine(format)
                sourceDataLine!!.open()
            }
        }
        loopStart = track.loopStart
        loopEnd = track.loopEnd
        loopCount = Clip.LOOP_CONTINUOUSLY
        togglePlayPause()
    }

    fun togglePlayPause() {
        isPlaying = !isPlaying
        if (isPlaying) {
            sourceDataLine!!.start()
            Apex.execute { playAudio() }
        } else {
            sourceDataLine!!.stop()
        }
    }

    val length
        get() = data!!.frameLength / frameRate

    var position
        get() = data!!.readPos / frameRate
        set(seconds) {
            data!!.readPos = (seconds * frameRate)
        }

    private fun playAudio() {
        while (isPlaying) {
            val limit = if (loopCount == 0) data!!.frameLength else loopEnd
            val len = min(limit - data!!.readPos, frameRate / 20)
            val b = data!!.readData(len)
            sourceDataLine!!.write(b, 0, b.size)
            if (data!!.readPos == loopEnd && loopCount != 0) {
                data!!.readPos = loopStart
                if (loopCount != Clip.LOOP_CONTINUOUSLY) loopCount--
            }
        }
    }
}
