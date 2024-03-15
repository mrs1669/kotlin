/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataCompilation

internal object IncorrectCompileOnlyDependenciesChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val multiplatform = multiplatformExtension ?: return

        val compilationsWithCompileOnlyDependencies = multiplatform.targets
            .filter { target -> !isAllowedCompileOnlyDependencies(target.platformType) }
            .flatMap { target -> compileOnlyDependencies(target) }
            .groupingBy { it.first }
            .fold(listOf<String>()) { acc, (_, dependencies) -> acc + dependencies }

        if (compilationsWithCompileOnlyDependencies.values.any { it.isNotEmpty() }) {
            project.reportDiagnostic(
                KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning(
                    compilationsWithCompileOnlyDependencies = compilationsWithCompileOnlyDependencies,
                )
            )
        }
    }

    private fun KotlinGradleProjectCheckerContext.isAllowedCompileOnlyDependencies(target: KotlinPlatformType): Boolean {
        return when (target) {
            KotlinPlatformType.jvm,
            KotlinPlatformType.androidJvm,
            -> true

            KotlinPlatformType.common,
            KotlinPlatformType.wasm,
            KotlinPlatformType.js,
            -> false

            KotlinPlatformType.native -> {
                @Suppress("DEPRECATION")
                PropertiesProvider(project).ignoreIncorrectNativeDependencies != true
            }
        }
    }

    /**
     * Extract all [target] dependencies that satisfy:
     * 1. defined as compileOnly
     * 2. are not also exposed as api elements.
     */
    private fun KotlinGradleProjectCheckerContext.compileOnlyDependencies(
        target: KotlinTarget,
    ): List<Pair<KotlinCompilation<*>, List<String>>> {
        val apiElementsDependencies = project.configurations.getByName(target.apiElementsConfigurationName).allDependencies

        fun Dependency.isInApiElements(): Boolean =
            apiElementsDependencies.any { it.contentEquals(this) }

        return target.compilations
            .filter { it.isPublished() }
            .map { compilation ->
                val compileOnlyDependencies = project.configurations.getByName(compilation.compileOnlyConfigurationName).allDependencies
                val nonApiCompileOnlyDependencies = compileOnlyDependencies.filter { !it.isInApiElements() }

                compilation to nonApiCompileOnlyDependencies.map { it.stringCoordinates() }
            }
    }

    /**
     * Estimate whether a [KotlinCompilation] is 'publishable' (i.e. it is a main, non-test compilation).
     */
    private fun KotlinCompilation<*>.isPublished(): Boolean {
        return when (this) {
            is KotlinMetadataCompilation<*> -> true
            else -> name == KotlinCompilation.MAIN_COMPILATION_NAME
        }
    }

    private fun Dependency.stringCoordinates(): String = buildString {
        group?.let { append(it).append(':') }
        append(name)
        version?.let { append(':').append(it) }
    }
}
