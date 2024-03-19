// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

@kotlin.ConsistentDataCopy
data class Data private constructor(val x: Int)
