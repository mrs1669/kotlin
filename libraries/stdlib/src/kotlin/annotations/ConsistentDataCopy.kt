/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * In future versions of Kotlin,
 * the generated 'copy' method of the data class will have the same visibility as visibility of the primary constructor.
 *
 * When you apply [ConsistentDataCopy] annotation to your data class:
 * 1. You enroll into the new behavior earlier.
 * 2. You suppress all the warnings/errors about the behavior change.
 *
 * ## Deprecation Timeline
 *
 * - **Phase 1.** Kotlin 2.1.
 *   The compiler warns about the behavior change on the data class declaration and on 'copy' method usages.
 * - **Phase 2.** (Supposedly Kotlin 2.2). The warning turns into an error.
 * - **Phase 3.** (Supposedly Kotlin 2.3). The default changes.
 *   Unless [InconsistentDataCopy] is used, the generated 'copy' method has the same visibility as visibility of the primary constructor.
 *   '-Xsafe-data-copy' compiler flag and [ConsistentDataCopy] annotation are now unnecessary.
 * - **Phase 4.** (Supposedly Kotlin 2.3 or 2.4). The compiler ignores [InconsistentDataCopy] annotation.
 *
 * ## Alternatives
 *
 * You have two alternatives:
 * - If you want all your data classes in the module to enrol into the new behavior earlier,
 *   you can use '-Xsafe-data-copy' compiler flag.
 * - If you want to provide more gradual migration of the generated 'copy' method, you can use [InconsistentDataCopy].
 *   See the [InconsistentDataCopy] documentation for more details.
 *
 * ## Recommendation
 *
 * - It's recommended to use '-Xsafe-data-copy' compiler flag for new modules.
 * - It's recommended to use [ConsistentDataCopy] annotation for all new data classes with non-public constructor.
 * - It's recommended to use [ConsistentDataCopy] annotation for all existing data classes
 *   in which you don't care about binary compatibility change of the generated 'copy' method.
 *
 * @see [InconsistentDataCopy]
 * @see [KT-11914](https://youtrack.jetbrains.com/issue/KT-11914)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@ExperimentalStdlibApi
@SinceKotlin("2.0")
public annotation class ConsistentDataCopy

/**
 * In future versions of Kotlin,
 * the generated 'copy' method of the data class will have the same visibility as visibility of the primary constructor.
 *
 * Please prefer [ConsistentDataCopy] annotation if you don't care about binary compatibility change of the generated 'copy' method.
 * See the [ConsistentDataCopy] documentation for more details.
 *
 * When you apply [InconsistentDataCopy] annotation to your data class:
 * 1. You choose to keep the public visibility of the generated 'copy' method for a longer migration period.
 *    But you still intend to migrate to the new behavior.
 * 2. You suppress the warning/error about the behavior change only on the declaration.
 *    **Please note** that the warning/error on all the **usages** of the 'copy' method **stays even if you use [InconsistentDataCopy]**!
 *
 * ## Deprecation Timeline
 *
 * - **Phase 1.** Kotlin 2.1.
 *   The compiler warns about the behavior change on the data class declaration and on 'copy' method usages.
 * - **Phase 2.** (Supposedly Kotlin 2.2). The warning turns into an error.
 * - **Phase 3.** (Supposedly Kotlin 2.3). The default changes.
 *   Unless [InconsistentDataCopy] is used, the generated 'copy' method has the same visibility as visibility of the primary constructor.
 *   '-Xsafe-data-copy' compiler flag and [ConsistentDataCopy] annotation are now unnecessary.
 * - **Phase 4.** (Supposedly Kotlin 2.3 or 2.4). The compiler ignores [InconsistentDataCopy] annotation.
 *
 * ## Alternatives
 *
 * - If you write new code or don't care about binary compatibility,
 *   it's recommended to use '-Xsafe-data-copy' compiler flag and [ConsistentDataCopy] annotation.
 *   See [ConsistentDataCopy] documentation for more details.
 * - You can introduce your own copy-like method alongside the generated 'copy',
 *   and migrate all the usages to the introduced method.
 * - You can rewrite your data class to regular Kotlin class.
 *   You need to manually implement all the data class generated methods.
 *
 * @see [ConsistentDataCopy]
 * @see [KT-11914](https://youtrack.jetbrains.com/issue/KT-11914)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
@ExperimentalStdlibApi
@SinceKotlin("2.0")
public annotation class InconsistentDataCopy
