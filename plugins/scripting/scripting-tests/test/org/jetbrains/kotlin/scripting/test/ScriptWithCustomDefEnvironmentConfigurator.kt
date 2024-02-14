/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.scripting.configuration.ScriptingConfigurationKeys
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import java.io.File

class ScriptWithCustomDefEnvironmentConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        System.getProperty("kotlin.script.test.script.definition.classpath")!!.split(File.pathSeparator).forEach {
            configuration.addJvmClasspathRoot(File(it).also { require(it.exists()) { "The file required for custom test script definition not found: $it" } })
        }
        module.directives[ScriptingTestDirectives.SCRIPT_DEFAULT_IMPORT].takeIf { it.isNotEmpty() }?.let {
            configuration.put(ScriptingConfigurationKeys.LEGACY_SCRIPT_RESOLVER_ENVIRONMENT_OPTION, "defaultImports", it)
        }
        super.configureCompilerConfiguration(configuration, module)
    }

    override val directiveContainers: List<DirectivesContainer> = listOf(ScriptingTestDirectives)
}