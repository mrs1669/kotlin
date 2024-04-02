plugins {
    kotlin("jvm")
    id("jps-compatible")
    application
}

val runtimeOnly by configurations
val compileOnly by configurations
runtimeOnly.extendsFrom(compileOnly)

dependencies {
    implementation(project(":generators"))
    implementation(project(":generators:tree-generator-common"))
    implementation(project(":compiler:util"))

    compileOnly(intellijCore())
    runtimeOnly(commonDependency("org.jetbrains.intellij.deps:jdom"))
}

application {
    mainClass.set("org.jetbrains.kotlin.bir.generator.MainKt")
}

sourceSets {
    "main" {
        projectDefault()
    }
    "test" {}
}

tasks.compileKotlin {
    compilerOptions {
        allWarningsAsErrors.set(false)
    }
}
