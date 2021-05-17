import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

group = "dev.feedforward.authorsupdate"
version = "0.0.1"

repositories {
    mavenCentral()
    maven { url =  uri("https://dl.bintray.com/jetbrains/markdown") }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("org.eclipse.jgit:org.eclipse.jgit:5.11.1.202105131744-r")
    implementation("org.kohsuke:github-api:1.128")
    implementation("org.jetbrains:markdown:0.1.45")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}