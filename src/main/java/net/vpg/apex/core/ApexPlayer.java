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

import javax.sound.sampled.*;
import java.io.IOException;

public class ApexPlayer {
    private SourceDataLine sourceDataLine;
    private int frameRate;
    private volatile AudioData data;
    private int loopStart;
    private int loopEnd;
    private int loopCount;
    private boolean playing;

    public void play(ApexTrack track) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        playing = false;
        if (data != null)
            data.close();
        data = track.getData();
        data.startCaching();
        AudioFormat format = data.getFormat();
        if (frameRate != format.getFrameRate()) {
            frameRate = (int) format.getFrameRate();
            if (sourceDataLine != null) {
                sourceDataLine.close();
                sourceDataLine.open(format);
            } else {
                sourceDataLine = AudioSystem.getSourceDataLine(format);
                sourceDataLine.open();
            }
        }
        loopStart = track.loopStart();
        loopEnd = track.loopEnd();
        loopCount = Clip.LOOP_CONTINUOUSLY;
        togglePlayPause();
    }

    public void togglePlayPause() {
        playing = !playing;
        if (playing) {
            sourceDataLine.start();
            Apex.EXECUTOR.execute(this::playAudio);
        } else {
            sourceDataLine.stop();
        }
    }

    public boolean isPlaying() {
        return playing;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int count) {
        loopCount = count;
    }

    public int getLength() {
        return data.getFrameLength() / frameRate;
    }

    public int getPosition() {
        return data.getReadPos() / frameRate;
    }

    public void setPosition(int seconds) {
        data.setReadPos(seconds * frameRate);
    }

    private void playAudio() {
        while (playing) {
            int limit = loopCount == 0 ? data.getFrameLength() : loopEnd;
            int len = Math.min(limit - data.getReadPos(), frameRate / 20);
            byte[] b = data.readData(len);
            sourceDataLine.write(b, 0, b.length);
            if (data.getReadPos() == loopEnd && loopCount != 0) {
                data.setReadPos(loopStart);
                if (loopCount != Clip.LOOP_CONTINUOUSLY)
                    loopCount--;
            }
        }
    }
}
