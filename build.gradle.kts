plugins {
    kotlin("jvm") version "1.5.10"
    id("org.openjfx.javafxplugin") version "0.0.8"
    application
}

javafx {
    modules("javafx.controls")
}

application {
    mainClass.set("MainKt")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("ch.qos.logback:logback-classic:1.2.5")

    testImplementation(platform("org.junit:junit-bom:5.10.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core:3.20.2")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
