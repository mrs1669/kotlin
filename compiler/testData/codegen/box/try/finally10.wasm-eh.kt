/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// JVM_ABI_K1_K2_DIFF: KT-63864
// WITH_STDLIB
// TARGET_BACKEND: WASM
// USE_NEW_EXCEPTION_HANDLING_PROPOSAL
// TODO: remove the test when KT-66906 will be resolved

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    while (true) {
        try {
            continue
        } finally {
            sb.appendLine("Finally")
            break
        }
    }

    sb.appendLine("After")

    assertEquals("""
        Finally
        After

    """.trimIndent(), sb.toString())
    return "OK"
}