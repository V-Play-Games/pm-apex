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

import java.io.File
import java.io.FileOutputStream
import java.net.URI

object Downloader {
    const val STARTED = 1
    const val IN_PROGRESS = 2
    const val DONE = 3

    fun download(
        url: String,
        filename: String = url.substring(url.lastIndexOf('/') + 1),
        listener: EventListener = EventListener { println(it) }
    ) = download(url, Resources.create(filename), listener)

    fun download(url: String, file: File, listener: EventListener = EventListener { println(it) }): File {
        val startingTime = System.nanoTime()
        var bytesRead = 0
        var len: Int
        URI.create(url).toURL().openStream().use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(4096)
                while ((input.read(buffer).also { len = it }) >= 0) {
                    bytesRead += len
                    output.write(buffer, 0, len)
                    val timeTaken = System.nanoTime() - startingTime
                    listener.progress(
                        Event(
                            file,
                            startingTime,
                            timeTaken,
                            len,
                            bytesRead.toLong(),
                            bytesRead * 100.0 / timeTaken,
                            if (bytesRead == len) STARTED else IN_PROGRESS
                        )
                    )
                }
            }
        }
        val timeTaken = System.nanoTime() - startingTime
        val speed = bytesRead * 100L / timeTaken
        listener.progress(Event(file, startingTime, timeTaken, len, bytesRead.toLong(), speed.toDouble(), DONE))
        return file
    }

    fun interface EventListener {
        fun progress(event: Event)
    }

    @JvmRecord
    data class Event(
        val file: File,
        val startingTime: Long,
        val timeTaken: Long,
        val bytesRead: Int,
        val totalBytesRead: Long,
        val speed: Double,
        val type: Int
    )
}
