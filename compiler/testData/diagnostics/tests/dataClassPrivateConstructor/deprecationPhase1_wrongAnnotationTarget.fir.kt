// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.ConsistentDataCopy<!>
class Foo

<!DATA_CLASS_CONSISTENT_COPY_WRONG_ANNOTATION_TARGET!>@kotlin.InconsistentDataCopy<!>
class Bar
