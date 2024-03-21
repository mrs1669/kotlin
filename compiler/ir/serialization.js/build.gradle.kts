plugins {
    kotlin("jvm")
    id("jps-compatible")
}

dependencies {
    api(project(":compiler:ir.psi2ir"))
    api(project(":compiler:fir:fir2ir"))
    api(project(":compiler:ir.serialization.common"))
    api(project(":js:js.frontend"))
    api(project(":wasm:wasm.config"))

    implementation(project(":compiler:ir.backend.common"))
    implementation(project(":compiler:fir:fir-serialization"))

    compileOnly(intellijCore())
}

optInToUnsafeDuringIrConstructionAPI()

sourceSets {
    "main" { projectDefault() }
}
