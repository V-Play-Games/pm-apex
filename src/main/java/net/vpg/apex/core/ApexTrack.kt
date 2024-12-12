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

import net.vpg.vjson.value.JSONArray
import net.vpg.vjson.value.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@JvmRecord
data class ApexTrack(
    val id: String,
    val name: String,
    val category: String,
    val frameLength: Int,
    val loopStart: Int,
    val loopEnd: Int
) {
    constructor(data: JSONObject) : this(
        data.getString("id"),
        data.getString("name"),
        data.getString("category"),
        data.getInt("frameLength"),
        data.getInt("loopStart"),
        data.getInt("loopEnd")
    )

    val data
        get() = file
            ?.takeIf { it.exists() }
            ?.let { AudioData(it, frameLength) }
            ?: AudioData(url, frameLength)

    val file
        get() = Resources["$id.ogg"]

    val url
        get() = URI.create(
            Resources.getProperty("additionalRes")
                .toString()
                .format(
                    category,
                    URLEncoder.encode(id, StandardCharsets.UTF_8).replace("+", "%20"),
                    "ogg"
                )
        ).toURL()

    init {
        logger.info("Loaded Track Info for ID: {}", id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApexTrack::class.java)
        val entries = JSONArray.parse(Resources["tracks.json"])
            .toList()
            .map { it.toObject() }
            .map { ApexTrack(it) }
            .associate { Pair(it.id, it) }
    }
}
