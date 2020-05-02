// TARGET_BACKEND: JVM_IR
// WITH_RUNTIME
fun one() = 1

fun box(): String {
    val uintProgression = 1u..7u
    for (i in uintProgression step one()) {
    }

    return "OK"
}

// For "step" progressions in JVM IR, a call to getProgressionLastElement() is made to compute the "last" value.
// If "step" is called on a non-literal progression, there is a check to see if that progression's step value is < 0.
// If the step is non-constant, there is a check that it is > 0, and if not, an IllegalArgumentException is thrown.
//
// Expected lowered form of loop:
//
//   // Additional variables:
//   val progression = intProgression
//   val nestedFirst = progression.first
//   val nestedLast = progression.last
//   val nestedStep = progression.step
//   val stepArg = one()
//   val checkedStep = if (stepArg > 0) stepArg
//                     else throw IllegalArgumentException("Step must be positive, was: $stepArg.")
//   val newStep = if (nestedStep > 0) checkedStep else -checkedStep
//
//   // Standard form of loop over progression
//   var inductionVar = nestedFirst
//   val last = getProgressionLastElement(nestedFirst, nestedLast, newStep)
//   val step = newStep
//   if ((step > 0 && inductionVar <= last) || (step < 0 && last <= inductionVar)) {
//     // Loop is not empty
//     do {
//       val i = inductionVar
//       inductionVar += step
//       // Loop body
//     } while (i != last)
//   }

// 0 iterator
// 0 getStart
// 0 getEnd
// 1 getFirst
// 1 getLast
// 1 getStep
// 1 INVOKESTATIC kotlin/internal/UProgressionUtilKt.getProgressionLastElement
// 1 NEW java/lang/IllegalArgumentException
// 1 ATHROW
// 2 INVOKESTATIC kotlin/UnsignedKt.uintCompare
// 1 IFGT
// 1 IF_ICMPNE
// 4 IFLE
// 1 IFGE
// 7 IF
// 1 INEG
// 0 INVOKESTATIC kotlin/UInt.constructor-impl
// 0 INVOKE\w+ kotlin/UInt.(un)?box-impl
