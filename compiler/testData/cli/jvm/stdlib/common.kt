// ISSUE: KT-65841

package kotlin

internal annotation class ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
expect open class Any() {
    public open fun hashCode(): Int

    public open fun toString(): String
}

@ActualizeByJvmBuiltinProvider
expect class Boolean

@ActualizeByJvmBuiltinProvider
expect class Int {
    companion object {
        val MIN_VALUE: Int
        val MAX_VALUE: Int
    }
}

@ActualizeByJvmBuiltinProvider
expect class String
