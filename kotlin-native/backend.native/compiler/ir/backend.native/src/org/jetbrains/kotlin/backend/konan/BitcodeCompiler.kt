/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.backend.konan.driver.PhaseContext
import org.jetbrains.kotlin.konan.TemporaryFilesService
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.*
import java.io.File

typealias BitcodeFile = String
typealias ObjectFile = String
typealias ExecutableFile = String

internal class BitcodeCompiler(
        private val context: PhaseContext,
) {

    private val config = context.config
    private val platform = config.platform
    private val optimize = context.shouldOptimize()
    private val debug = config.debug

    private val overrideClangOptions =
            config.configuration.getList(KonanConfigKeys.OVERRIDE_CLANG_OPTIONS)

    private fun MutableList<String>.addNonEmpty(elements: List<String>) {
        addAll(elements.filter { it.isNotEmpty() })
    }

    private fun runTool(vararg command: String) =
            Command(*command)
                    .logWith(context::log)
                    .execute()

    private fun targetTool(tool: String, vararg arg: String) {
        val absoluteToolName = if (platform.configurables is AppleConfigurables) {
            "${platform.absoluteTargetToolchain}/usr/bin/$tool"
        } else {
            "${platform.absoluteTargetToolchain}/bin/$tool"
        }
        runTool(absoluteToolName, *arg)
    }

    private fun hostLlvmTool(tool: String, vararg arg: String) {
        val absoluteToolName = "${platform.absoluteLlvmHome}/bin/$tool"
        runTool(absoluteToolName, *arg)
    }

    private fun clang(configurables: ClangFlags, inputFile: File, outputFile: File): File {
        val targetTriple = if (configurables is AppleConfigurables) {
            platform.targetTriple.withOSVersion(configurables.osVersionMin)
        } else {
            platform.targetTriple
        }
        val flags = overrideClangOptions.takeIf(List<String>::isNotEmpty)
                ?: mutableListOf<String>().apply {
                    addNonEmpty(configurables.clangFlags)
                    addNonEmpty(listOf("-triple", targetTriple.toString()))
                    if (configurables is ZephyrConfigurables) {
                        addNonEmpty(configurables.constructClangCC1Args())
                    }
                    addNonEmpty(when {
                        optimize -> configurables.clangOptFlags
                        debug -> configurables.clangDebugFlags
                        else -> configurables.clangNooptFlags
                    })
                    addNonEmpty(BitcodeEmbedding.getClangOptions(config))
                    addNonEmpty(configurables.currentRelocationMode(context).translateToClangCc1Flag())
                }
        if (configurables is AppleConfigurables) {
            targetTool("clang++", *flags.toTypedArray(), inputFile.absolutePath, "-o", outputFile.absolutePath)
        } else {
            hostLlvmTool("clang++", *flags.toTypedArray(), inputFile.absolutePath, "-o", outputFile.absolutePath)
        }
        return outputFile
    }

    private fun RelocationModeFlags.Mode.translateToClangCc1Flag() = when (this) {
        RelocationModeFlags.Mode.PIC -> listOf("-mrelocation-model", "pic")
        RelocationModeFlags.Mode.STATIC -> listOf("-mrelocation-model", "static")
        RelocationModeFlags.Mode.DEFAULT -> emptyList()
    }

    fun makeObjectFile(bitcodeFile: File, outputFile: File): File =
            when (val configurables = platform.configurables) {
                is ClangFlags -> clang(configurables, bitcodeFile, outputFile)
                else -> error("Unsupported configurables kind: ${configurables::class.simpleName}!")
            }
}