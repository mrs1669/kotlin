// MODULE: m1-common
// FILE: common.kt

expect class <!PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!>(i: Int) {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public Foo(int i) {}
    public void foo() {}
}

// FILE: jvm.kt

class <!ACTUAL_MISSING, PACKAGE_OR_CLASSIFIER_REDECLARATION!>Foo<!><T>(t: T) {
    fun <!ACTUAL_MISSING!>foo<!>() {}
}
