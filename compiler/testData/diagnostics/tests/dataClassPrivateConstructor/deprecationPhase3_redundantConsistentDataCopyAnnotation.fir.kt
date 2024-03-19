// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

<!REDUNDANT_ANNOTATION!>@kotlin.ConsistentDataCopy<!>
data class Data private constructor(val x: Int)
