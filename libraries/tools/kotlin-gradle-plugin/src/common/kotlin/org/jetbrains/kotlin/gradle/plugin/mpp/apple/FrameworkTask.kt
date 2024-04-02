/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple

import org.gradle.api.DefaultTask
import org.gradle.api.file.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.utils.getFile
import org.jetbrains.kotlin.gradle.utils.processPlist
import org.jetbrains.kotlin.incremental.createDirectory
import org.jetbrains.kotlin.incremental.deleteRecursivelyOrThrow
import org.jetbrains.kotlin.konan.target.HostManager
import java.io.File
import java.io.Serializable
import javax.inject.Inject

internal data class FrameworkModule(val name: String, val header: File) : Serializable

@DisableCachingByDefault
internal abstract class FrameworkTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val providerFactory: ProviderFactory,
) : DefaultTask() {
    init {
        onlyIf { HostManager.hostIsMac }
    }

    @get:SkipWhenEmpty
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val binary: RegularFileProperty

    @get:Optional
    @get:Input
    abstract val modules: ListProperty<FrameworkModule>

    @get:Input
    abstract val frameworkName: Property<String>

    @get:Input
    abstract val bundleIdentifier: Property<String>

    @get:Input
    val platformName: Provider<String>
        get() = providerFactory.environmentVariable("PLATFORM_NAME")

    @get:Internal
    abstract val frameworkPath: DirectoryProperty

    @get:OutputDirectory
    val frameworkRootPath: Provider<Directory>
        get() = frameworkPath.map { it.dir("${frameworkName.get()}.framework") }

    private val modulePath: Provider<Directory>
        get() = frameworkRootPath.map { it.dir("Modules") }

    private val headerPath: Provider<Directory>
        get() = frameworkRootPath.map { it.dir("Headers") }

    @TaskAction
    fun assembleFramework() {
        frameworkRootPath.getFile().apply {
            if (exists()) {
                deleteRecursivelyOrThrow()
            }
        }

        modulePath.getFile().createDirectory()
        headerPath.getFile().createDirectory()

        copyBinary()
        copyHeaders()
        createModuleMap()
        createInfoPlist()
    }

    private fun copyBinary() {
        binary.getFile().copyTo(
            frameworkRootPath.getFile().resolve(frameworkName.get())
        )
    }

    private fun createModuleMap(umbrella: File? = null) {
        val umbrellaHeader = umbrella?.let { "umbrella header \"${it.name}\"" }
        val modules = modules.getOrElse(emptyList())

        modulePath.getFile().resolve("module.modulemap").writeText(
            """
            |framework module ${frameworkName.get()} {
            |   ${umbrellaHeader.orEmpty()}
            |   export *
            |   
            |${
                modules.joinToString("\n") {
                    """
                    |   module ${it.name} {
                    |       header "${it.header.name}"
                    |       export *
                    |   }
                    """.trimMargin()
                }
            }
            |
            |   link "swiftExportBinary"
            |   
            |   use Foundation
            |   requires objc    
            |}
            """.trimMargin()
        )
    }

    private fun createInfoPlist() {
        val info = mapOf(
            "CFBundleIdentifier" to bundleIdentifier.get(),
            "CFBundleInfoDictionaryVersion" to "6.0",
            "CFBundlePackageType" to "FMWK",
            "CFBundleVersion" to "1",
            "DTSDKName" to platformName.get(),
            "CFBundleExecutable" to frameworkName.get(),
            "CFBundleName" to frameworkName.get()
        )

        val outputFile = frameworkRootPath.getFile().resolve("Info.plist")

        processPlist(outputFile, execOperations) {
            info.forEach {
                add(":${it.key}", it.value)
            }
        }
    }

    private fun copyHeaders() {
        modules.getOrElse(emptyList()).map { it.header }.forEach {
            it.copyTo(
                headerPath.getFile().resolve(it.name)
            )
        }
    }
}