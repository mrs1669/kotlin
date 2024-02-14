/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.test

import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer

object ScriptingTestDirectives : SimpleDirectivesContainer() {
    val SCRIPT_DEFAULT_IMPORT by stringDirective("Default imports", multiLine = true)
}