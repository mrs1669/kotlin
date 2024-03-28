// ISSUE: KT-65841

package kotlin

internal annotation class ActualizeByJvmBuiltinProvider

@ActualizeByJvmBuiltinProvider
public expect abstract class Enum<E : Enum<E>> : Comparable<E> {
}
