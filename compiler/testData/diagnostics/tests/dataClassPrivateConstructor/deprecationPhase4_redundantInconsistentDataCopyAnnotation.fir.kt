// WITH_STDLIB
// LANGUAGE: +IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

<!REDUNDANT_ANNOTATION!>@kotlin.InconsistentDataCopy<!>
data class Data private constructor(val x: Int)
