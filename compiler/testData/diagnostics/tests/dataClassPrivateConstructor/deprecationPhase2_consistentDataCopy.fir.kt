// WITH_STDLIB
// LANGUAGE: +ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@OptIn(ExperimentalStdlibApi::class)
@kotlin.ConsistentDataCopy
data class Data private constructor(val x: Int)

fun local(data: Data) {
    data.<!INVISIBLE_REFERENCE!>copy<!>()
}
