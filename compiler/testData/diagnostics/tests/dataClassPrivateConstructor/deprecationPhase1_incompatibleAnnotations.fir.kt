// WITH_STDLIB
// LANGUAGE: +WarnAboutDataClassCopyVisibilityChange, -ErrorAboutDataClassCopyVisibilityChange, -DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

<!DATA_CLASS_CONSISTENT_COPY_AND_INCONSISTENT_COPY_ARE_INCOMPATIBLE_ANNOTATIONS!>@kotlin.ConsistentDataCopy<!>
<!DATA_CLASS_CONSISTENT_COPY_AND_INCONSISTENT_COPY_ARE_INCOMPATIBLE_ANNOTATIONS!>@kotlin.InconsistentDataCopy<!>
data class Data private constructor(val x: Int)
