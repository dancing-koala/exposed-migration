import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
    maven
}

group = "fr.dancing_koala"
version = "0.0.3"

repositories {
    mavenCentral()
}

val exposedVersion: String by project
dependencies {
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")

    testImplementation("com.h2database:h2:1.4.200")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")

    testImplementation(kotlin("test-junit"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}