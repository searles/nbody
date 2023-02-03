import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.21"
    id("org.openjfx.javafxplugin") version "0.0.8"
    application
}

javafx {
    version = "11.0.2"
    modules = listOf("javafx.controls", "javafx.graphics")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("org.tensorflow:tensorflow-core-platform-gpu:0.3.0")
    implementation("org.openjfx:javafx-base:14.0.1")
    implementation("org.openjfx:javafx-graphics:14.0.1")
    implementation("org.apache.commons:commons-math3:3.7")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("org.jetbrains:annotations:13.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("searles.MainKt")
}