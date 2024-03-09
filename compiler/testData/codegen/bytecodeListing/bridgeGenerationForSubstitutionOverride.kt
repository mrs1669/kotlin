// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

// FILE: A.java

public class A<T> {
    public int length() {
        return 1;
    }
}

// FILE: B.java

public interface B {
    public int length();
}

// FILE: JavaClass.java

import org.jetbrains.annotations.NotNull;

public class JavaClass extends A<String> implements B, CharSequence {
    @Override
    public char charAt(int index) {
        return 0;
    }

    @NotNull
    @Override
    public CharSequence subSequence(int start, int end) {
        return "";
    }
}

// FILE: main.kt

class KotlinImpl : JavaClass()