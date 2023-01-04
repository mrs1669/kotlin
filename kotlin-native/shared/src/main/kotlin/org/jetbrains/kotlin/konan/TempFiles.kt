/*
 * Copyright 2010-2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.kotlin.konan

import java.io.File
import java.nio.file.Files

interface TemporaryFilesService {
    fun create(name: String): File

    fun dispose()
}

/**
 * Creates and stores temporary compiler outputs
 * If pathToTemporaryDir is given and is not empty then temporary outputs will be preserved
 */
class TempFiles(pathToTemporaryDir: String? = null) : TemporaryFilesService {
    override fun dispose() {
        if (deleteOnExit) {
            // Note: this can throw an exception if a file deletion is failed for some reason (e.g. OS is Windows and the file is in use).
            dir.deleteRecursively()
        }
    }

    private val deleteOnExit = pathToTemporaryDir.isNullOrEmpty()

    private val dir by lazy {
        if (deleteOnExit) {
            Files.createTempDirectory("konan_temp").toFile()
        } else {
            createDirForTemporaryFiles(pathToTemporaryDir!!)
        }
    }

    private fun createDirForTemporaryFiles(path: String): File {
        if (File(path).isFile) {
            throw IllegalArgumentException("Given file is not a directory: $path")
        }
        return File(path).apply {
            if (!exists()) { mkdirs() }
        }
    }

    /**
     * Create file named [name] inside temporary dir
     */
    override fun create(name: String): File =
            File(dir, name)
}

