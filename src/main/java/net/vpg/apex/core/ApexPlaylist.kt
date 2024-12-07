package net.vpg.apex.core

import javax.swing.DefaultListModel

class ApexPlaylist(val category: String, private val tracks: List<ApexTrack>) {
    companion object {
        val EMPTY = ApexPlaylist("", emptyList<ApexTrack>())
    }
    val model = DefaultListModel<String>().apply {
        addAll(tracks.map { it.name })
    }
    val size
        get() = tracks.size
    operator fun get(index: Int) = tracks[index]
}
