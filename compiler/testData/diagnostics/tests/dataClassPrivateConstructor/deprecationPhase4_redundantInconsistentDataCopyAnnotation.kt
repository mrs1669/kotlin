// WITH_STDLIB
// LANGUAGE: +IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

@kotlin.InconsistentDataCopy
data class Data private constructor(val x: Int)
