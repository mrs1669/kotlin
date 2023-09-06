// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect open class A() {
    fun foo()

    val x: Int
}

open class B : A()

// MODULE: m1-jvm(m1-common)
// FILE: jvm.kt

actual open <!ACTUAL_CLASSIFIER_MUST_HAVE_THE_SAME_MEMBERS_AS_NON_FINAL_EXPECT_CLASSIFIER!>class A<!> {
    actual fun foo() {}

    fun <!NON_ACTUAL_MEMBER_DECLARED_IN_EXPECT_NON_FINAL_CLASSIFIER_ACTUALIZATION!>bar<!>() {}

    actual val x = 42
}

class C : B() {
    fun test() {
        foo()
        bar()
        x + x
    }
}

class D : A() {
    fun test() {
        foo()
        bar()
        x + x
    }
}
