// TARGET_BACKEND: WASM
// IGNORE_BACKEND: WASM
// Ignore reason KT-xxxxx

fun f1(x: Number): String = x.toString()
fun f2(x: Number): String = x.toString()

fun floatLambda(f: (Float) -> String): String = js("f(42.2)")
fun byteLambda(f: (Byte) -> String): String =  js("f(42)")

fun box(): String {

    val f1ref = ::f1
    val resultAsFloat1 = floatLambda(f1ref)
    val resultAsByte1 = byteLambda(f1ref)

    val f2ref = ::f2
    val resultAsByte2 = byteLambda(f2ref)
    val resultAsFloat2 = floatLambda(f2ref)

    if (resultAsFloat1 != resultAsFloat2) return "FAIL1"
    if (resultAsByte1 != resultAsByte2) return "FAIL2"

    if (resultAsFloat1 == resultAsByte1) return "FAIL3"
    if (resultAsFloat2 == resultAsByte2) return "FAIL4"

    return "OK"
}