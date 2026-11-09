plugins {
    id("java-library")
    kotlin("jvm") version "2.4.21"
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.gradleup.shadow") version "9.6.1"
    id("com.google.devtools.ksp") version "2.3.10"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io") // Vault
}

dependencies {
    compileOnly("com.discord4j:discord4j-core:3.3.3")
    compileOnly("com.github.TBMCPlugins.ChromaCore:Chroma-Core:v2.0.0-SNAPSHOT")
    compileOnly("com.vdurmont:emoji-java:5.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.11.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.23.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.23.0")
}
