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
import net.vpg.apex.Util.deepListFiles
import net.vpg.vjson.value.JSONObject
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException
import java.nio.file.Paths

object Resources {
    // init basic JSON info
    private val logger = LoggerFactory.getLogger(Resources::class.java)
    private val properties = JSONObject.parse(Apex::class.java.getResource("info.json"))
    private val dataDir: File
    private val resources: MutableMap<String, File>

    init {
        // init directories
        val os = System.getProperty("os.name")
        val home = System.getProperty("user.home")
        val dataPath = if (os.contains("Mac"))
            Paths.get(home, "Library", "Application Support")
        else if (os.contains("Windows"))
            getPathFromEnv("LOCALAPPDATA", false, home, "AppData", "Local")
        else // Linux/Unix
            getPathFromEnv("XDG_DATA_HOME", true, home, ".local", "share")
        dataDir = dataPath.resolve(properties.getString("appName")).toFile()
            .also { it.mkdirs() }
        properties.getArray("required")
            .toList()
            .map { it.toString() }
            .forEach { this.shiftFile(it) }
        resources = dataDir.deepListFiles().associate { Pair(it.getName(), it) }.toMutableMap()
    }

    private fun getPathFromEnv(envVar: String, mustBeAbsolute: Boolean, first: String, vararg more: String) =
        System.getenv(envVar)
            .takeIf { it.isNotEmpty() }
            ?.let { Paths.get(it) }
            ?.takeIf { !mustBeAbsolute || it.isAbsolute }
            ?: Paths.get(first, *more).also {
                logger.warn("{} not defined in environment, falling back on \"{}\"", envVar, it)
            }

    private fun shiftFile(resource: String) {
        try {
            Apex::class.java.getResourceAsStream(resource).use { input ->
                File(dataDir, resource).outputStream().use { output ->
                    input!!.transferTo(output)
                }
            }
        } catch (e: IOException) {
            logger.error("Unable to copy {} to the resource directory", resource, e)
        }
    }

    operator fun get(filename: String) = resources[filename]

    fun getProperty(prop: String) = properties.get(prop)

    fun create(filename: String) = File(dataDir, filename).also { resources.put(filename, it) }
}
