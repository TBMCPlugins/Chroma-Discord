// Needed for the tests (because of Vault)
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("org.bukkit:bukkit") {
        select("io.papermc.paper:paper-api:26.3.build.+")
    }
}

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
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // Vault
    maven("https://repo.essentialsx.net/releases/") // EssentialsX
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("com.discord4j:discord4j-core:3.3.3")
    compileOnly("org.chromagaming:Chroma-Core:2.0-SNAPSHOT")
    compileOnly("com.vdurmont:emoji-java:5.1.1")
    compileOnly("net.essentialsx:EssentialsX:2.21.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive:1.11.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-api:2.23.0")
    implementation("com.github.shynixn.mccoroutine:mccoroutine-bukkit-core:2.23.0")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    jvmToolchain(25)
}
