/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics.IncorrectCompileOnlyDependencyWarning
import org.jetbrains.kotlin.gradle.plugin.diagnostics.kotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.util.*
import kotlin.test.Test

@OptIn(ExperimentalWasmDsl::class)
class MultiplatformIncorrectCompileOnlyDependenciesValidationTest {

    private fun setupProject(configure: Project.() -> Unit): Project {
        val project = buildProjectWithMPP {
            kotlin {
                jvm()

                linuxX64()
                mingwX64()
                macosX64()

                js { browser() }

                wasmJs { browser() }
                wasmWasi { nodejs() }
            }

            configure()
        }

        return project.evaluate()
    }

    @Test
    fun `when compileOnly dependency is not defined anywhere, expect no warning`() {
        val project = setupProject {}

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    /**
     * The `compileOnly()` warning is only relevant for 'published' compilations.
     *
     * Verify `compileOnly()` dependencies in test sources do not trigger the warning.
     */
    @Test
    fun `when compileOnly dependency is defined in commonTest, expect no warning`() {
        val project = setupProject {
            kotlin {
                sourceSets.apply {
                    commonTest {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    @Test
    fun `when dependency is defined as compileOnly but not api, expect warnings`() {
        val project = setupProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = project.kotlinToolingDiagnosticsCollector
                .getDiagnosticsForProject(project)

            diagnostics.assertContainsSingleDiagnostic(IncorrectCompileOnlyDependencyWarning)

            val diag = diagnostics.first()
            assertMatches(diag.message, compileOnlyDependencyWarningRegex)
        }

        // TODO more validation:
//        diagnostics.assertDiagnostics(ToolingDiagnostic("x", "x", ToolingDiagnostic.Severity.ERROR))

//            .runLifecycleAwareTest {
//
//            configurationResult.await()
//            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)

//            diagnostics.assertNoDiagnostics("")

//            build("help") {
//                val warnings =
//                    org.jetbrains.kotlin.gradle.unitTests.MultiplatformIncorrectCompileOnlyDependenciesValidationIT.compileOnlyDependencyWarningRegex.findAll(
//                        output
//                    )
//                        .map {
//                            val (platformName) = it.destructured
//                            platformName
//                        }
//                        .distinct()
//                        .toList()
//                        .sorted()
//
//                assertContentEquals(
//                    listOf(
//                        "Kotlin/JS",
//                        "Kotlin/Native",
//                        "Kotlin/Wasm",
//                    ),
//                    warnings,
//                    message = "expect warnings for compileOnly-incompatible platforms"
//                )
//            }
//        }
    }


    @Test
    fun `when commonMain dependency is defined as compileOnly and api, expect no warning`() {
        val project = setupProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    @Test
    fun `when dependency is defined as compileOnly in commonMain, and api in target main sources, expect no warning`() {
        val project = setupProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                    listOf(
                        jvmMain,
                        jsMain,
                        nativeMain,
                        wasmJsMain,
                        wasmWasiMain,
                    ).forEach {
                        it.dependencies {
                            api("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }

    }

    @Test
    fun `when dependency is defined as compileOnly but not api, and kotlin-mpp warning is disabled, expect no warning`() {
        val project = setupProject {
            kotlin {
                sourceSets.apply {
                    commonMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }

            propertiesExtension.set("kotlin.suppressGradlePluginWarnings", "IncorrectCompileOnlyDependencyWarning")
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    @Test
    fun `when dependency is defined in nativeMain as compileOnly but not api, and kotlin-native warning is disabled, expect no warning for native compilations`() {
        val project = setupProject {
            kotlin {
                sourceSets.apply {
                    nativeMain {
                        dependencies {
                            compileOnly("org.jetbrains.kotlinx:atomicfu:latest.release")
                        }
                    }
                }
            }
            propertiesExtension.set("kotlin.native.ignoreIncorrectDependencies", "true")
        }

        project.runLifecycleAwareTest {
            val diagnostics = kotlinToolingDiagnosticsCollector.getDiagnosticsForProject(this)
            diagnostics.assertNoDiagnostics(IncorrectCompileOnlyDependencyWarning)
        }
    }

    companion object {
        private val compileOnlyDependencyWarningRegex = Regex(
            "A compileOnly dependency is used in the (?<platformName>[^ ]*) target '[^']*':"
        )
    }
}
