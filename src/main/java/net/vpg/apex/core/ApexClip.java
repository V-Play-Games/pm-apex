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

public class ApexClip {
    private SourceDataLine sourceDataLine;
    private AudioFormat format;
    private volatile AudioData data;
    private int loopStart;
    private int loopEnd;
    private int loopCount;
    private boolean playing;
    private boolean stopped = true;

    public void play(Track track) throws UnsupportedAudioFileException, IOException, LineUnavailableException {
        playing = false;
        if (data != null)
            data.close();
        data = track.getData();
        data.startCaching();
        if (format != data.getFormat()) {
            format = data.getFormat();
            if (sourceDataLine != null)
                sourceDataLine.close();
            sourceDataLine = AudioSystem.getSourceDataLine(format);
            sourceDataLine.open();
        }
        loopStart = track.loopStart();
        loopEnd = track.loopEnd();
        loopCount = Clip.LOOP_CONTINUOUSLY;
        togglePlayPause();
    }

    public void togglePlayPause() {
        playing = !playing;
        if (playing) {
            stopped = false;
            sourceDataLine.start();
            Apex.EXECUTOR.execute(this::playAudio);
        } else {
            sourceDataLine.stop();
        }
    }

    public void stop() {
        if (stopped)
            return;
        playing = false;
        stopped = true;
        data.setReadPos(0);
        sourceDataLine.flush();
        sourceDataLine.stop();
    }

    public boolean isPlaying() {
        return playing;
    }

    public boolean isStopped() {
        return stopped;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public void setLoopCount(int count) {
        loopCount = count;
    }

    public int getMicrosecondLength() {
        return (int) (data.getFrameLength() * 1000_000 / format.getSampleRate());
    }

    public int getMicrosecondPosition() {
        return (int) (data.getReadPos() * 1000_000 / format.getSampleRate());
    }

    public void setMicrosecondPosition(int microseconds) {
        data.setReadPos((int) (microseconds / 1000_000 * format.getSampleRate()));
    }

    private void playAudio() {
        int frameRate = (int) format.getFrameRate();
        int frameSize = format.getFrameSize();
        while (playing) {
            int limit = loopCount == 0 ? data.getFrameLength() : loopEnd;
            int len = Math.min(limit - data.getReadPos(), frameRate / 20); // push at most 50 ms of audio
            byte[] b = data.readData(len);
            sourceDataLine.write(b, 0, len * frameSize);
            if (data.getReadPos() == loopEnd && loopCount != 0) {
                data.setReadPos(loopStart);
                if (loopCount != Clip.LOOP_CONTINUOUSLY)
                    loopCount--;
            }
        }
    }
}
