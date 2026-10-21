plugins {
    kotlin("jvm") version "2.2.20"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "elehxystudio"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)
}

javafx {
    version = "21"
    modules("javafx.controls")
}

application {
    mainClass.set("elehxystudio.launcher.MainKt")
}
