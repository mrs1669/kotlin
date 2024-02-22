// WITH_STDLIB
// LANGUAGE: +WarnAboutDataClassCopyVisibilityChange, -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

@kotlin.ConsistentDataCopy
@kotlin.InconsistentDataCopy
data class Data private constructor(val x: Int)
