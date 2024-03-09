// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// FILE: A.java

public class A {
    public int length() {
        return 1;
    }
}

// FILE: interfaces.kt
interface LengthInterface {
    val length: Int
}

interface DiamondInterface : LengthInterface, CharSequence

// FILE: JavaImpl.java

import org.jetbrains.annotations.NotNull;

public class JavaImpl extends A implements DiamondInterface {
    @Override
    public char charAt(int index) {
        return 0;
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return "";
    }

    @Override
    public int getLength() {
        return 3;
    }
}

// FILE: main.kt

class KotlinImpl : JavaImpl()