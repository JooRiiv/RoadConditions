import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
    kotlin("plugin.serialization") version "1.9.0"
}

group = "com.example"
version = "0.0.1"

application {
    mainClass.set("com.example.ApplicationKt")
}

dependencies {
    implementation("io.ktor:ktor-server-core-jvm:3.2.3")
    implementation("io.ktor:ktor-server-netty-jvm:3.2.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.2.3")
    implementation("ch.qos.logback:logback-classic:1.4.11")
    implementation("org.mongodb:mongodb-driver-sync:4.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.ktor:ktor-server-call-logging-jvm:3.2.3")

    testImplementation("io.ktor:ktor-server-test-host:3.2.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.0")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile> {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

apply(plugin = "com.github.johnrengelman.shadow")

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("ktor-backend")
    archiveClassifier.set("")
    archiveVersion.set(version.toString())
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}
