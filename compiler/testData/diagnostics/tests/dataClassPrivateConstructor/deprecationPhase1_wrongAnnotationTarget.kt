// WITH_STDLIB
// LANGUAGE: +DataClassCopyRespectsConstructorVisibility, -IgnoreInconsistentDataCopyAnnotation
@file:OptIn(ExperimentalStdlibApi::class)

@kotlin.ConsistentDataCopy
class Foo

@kotlin.InconsistentDataCopy
class Bar
