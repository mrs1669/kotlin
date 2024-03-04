// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

value class Foo(val value: Int)
// CHECK-LABEL: define %struct.ObjHeader* @"kfun:Foo#$<bridge-NUN>toString(){}kotlin.String(){}kotlin.String
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-LABEL: call %struct.ObjHeader* @"kfun:Foo#toString(){}kotlin.String
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define i32 @"kfun:#foo(kotlin.Any){}kotlin.Int
fun foo(x: Any) = x as Int
// CHECK-DEBUG: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG-NOT: {{call|call zeroext}} i1 @IsSubtype
// CHECK-OPT-NOT: {{call|call zeroext}} i1 @IsSubclassFast
// CHECK-DEBUG: call i32 @"kfun:kotlin#<Int-unbox>(kotlin.Any){}kotlin.Int
// CHECK-OPT: bitcast %struct.ObjHeader* {{%[0-9]+}} to %"kclassbody:kotlin.Int#internal"
// CHECK-LABEL: epilogue:

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    println(Foo(42))
    println(foo(42))
    return "OK"
}
