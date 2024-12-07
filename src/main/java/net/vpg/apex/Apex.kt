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
package net.vpg.apex

import net.vpg.apex.core.ApexPlayer
import net.vpg.apex.core.ApexPlaylist
import net.vpg.apex.core.ApexTrack
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.min

object Apex {
    private val executor = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Apex ", 1).factory())
    private val player = ApexPlayer()
    private val window = ApexWindow(this)
    private val playlists = ApexTrack.entries
        .values
        .sortedBy { it.id }
        .groupBy { it.category }
        .entries
        .associate { Pair(it.key, ApexPlaylist(it.key, it.value)) }
    private var playlistPlaying = ApexPlaylist.EMPTY
    private var playlistShown = ApexPlaylist.EMPTY
    private var index = -1
    private var shuffle = false

    init {
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(Runnable {
            if (player.isPlaying) {
                val len = player.length
                if (window.seekBar.valueIsAdjusting) {
                    val seek = player.length * window.seekBar.value / 10000
                    window.progress.text = "%02d:%02d / %02d:%02d".format(seek / 60, seek % 60, len / 60, len % 60)
                } else {
                    val pos = player.position
                    window.seekBar.value = pos * 10000 / len
                    window.progress.text = "%02d:%02d / %02d:%02d".format(pos / 60, pos % 60, len / 60, len % 60)
                }
            }
        }, 50, 50, TimeUnit.MILLISECONDS)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        window.isVisible = true
    }

    fun execute(command: () -> Unit) = executor.execute(command)

    fun takeAction(action: Int) = execute {
        when (action) {
            Action.NEXT -> updateTrack(index + 1, false)
            Action.PREVIOUS -> updateTrack(index - 1, false)
            Action.SHUFFLE -> window.shuffleButton.text = "Shuffle ${if (!shuffle) " ON " else " OFF "}".also {
                shuffle = !shuffle
            }

            Action.PLAY_PAUSE -> player.togglePlayPause()
            Action.SEARCH -> search(if (playlistShown === playlistPlaying) index + 1 else 0, playlistShown.size)
            Action.CLICK_ON_PLAYLIST -> updateTrack(window.trackList.selectedIndex, true)
            Action.UPDATE_CATEGORY -> updatePlaylistShown(
                playlists[window.categories.selectedItem?.toString()] ?: ApexPlaylist.EMPTY
            )

            Action.PROGRESS_SEEK -> updateProgress()
        }
        updateButtons()
    }

    private fun search(start: Int, end: Int) {
        val searchText = window.searchTextArea.text.lowercase().replace("\n", "")
        for (i in start until end) {
            if (playlistShown[i].id.lowercase().contains(searchText)) {
                updateScrollBar(i)
                window.searchTextArea.text = ""
                return
            }
        }
        if (start != 0) {
            search(0, start)
        }
    }

    private fun updateProgress() {
        if (window.seekBar.isEnabled) {
            player.position = player.length * window.seekBar.value / 10000
        }
    }

    private fun updatePlaylistShown(playlist: ApexPlaylist) {
        if (playlistShown === playlist) return
        playlistShown = playlist
        window.trackList.model = playlistShown.model
        window.categories.selectedItem = playlistShown.category
    }

    private fun updateTrack(i: Int, playlistClick: Boolean) {
        if (i < 0) return
        if (playlistClick) playlistPlaying = playlistShown
        else updatePlaylistShown(playlistPlaying)
        window.seekBar.isEnabled = false
        window.trackName.text = "Loading..."
        index = if (playlistClick || !shuffle) i else (Math.random() * playlistPlaying.size).toInt()
        val track = playlistPlaying[index]
        updateScrollBar(index)
        player.play(track)
        window.seekBar.isEnabled = true
        window.trackName.text = "Now Playing: %s (%d/%d)".format(track.name, index + 1, playlistPlaying.size)
    }

    private fun updateButtons() {
        if (index == -1) return
        window.next.isEnabled = shuffle || index != playlistPlaying.size - 1
        window.previous.isEnabled = shuffle || index != 0
        window.seekBar.isEnabled = true
        window.playPause.isEnabled = true
        window.playPause.text = if (player.isPlaying) "Pause" else "Play"
    }

    private fun updateScrollBar(i: Int) {
        window.trackList.selectedIndex = i
        val scrollBar = window.trackListPane.verticalScrollBar
        val rowHeight = scrollBar.maximum / playlistShown.size
        val firstVisibleIndex = scrollBar.value / rowHeight
        val visibleAmount = scrollBar.visibleAmount / rowHeight
        if (i < firstVisibleIndex) {
            scrollBar.value = i * rowHeight
        } else if (i > firstVisibleIndex + visibleAmount - 1) {
            scrollBar.value = min(i - visibleAmount + 1, playlistShown.size - visibleAmount + 1) * rowHeight
        }
    }

    object Action {
        const val NEXT = 1
        const val PREVIOUS = 2
        const val SHUFFLE = 3
        const val PLAY_PAUSE = 5
        const val SEARCH = 6
        const val CLICK_ON_PLAYLIST = 7
        const val UPDATE_CATEGORY = 8
        const val PROGRESS_SEEK = 9
    }
}
