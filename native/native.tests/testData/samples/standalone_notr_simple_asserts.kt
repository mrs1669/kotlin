// KIND: STANDALONE_NO_TR

import kotlin.test.*

fun main() {
    assertFailsWith<AssertionError> {
        @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
        assert(false)
    }
}