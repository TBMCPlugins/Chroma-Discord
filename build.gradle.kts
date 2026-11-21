import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
    id("com.google.devtools.ksp") version "2.3.10"
    id("com.gradleup.shadow") version "9.6.1"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // Vault
    maven("https://repo.essentialsx.net/releases/") // EssentialsX
}

val runPlugins = configurations.create("runPlugins") { isTransitive = false }

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.+")
    compileOnly("org.chromagaming:Chroma-Core:2.0-SNAPSHOT")
    runPlugins("org.chromagaming:Chroma-Core:2.0-SNAPSHOT")
    compileOnly("net.essentialsx:EssentialsX:2.21.2")
    implementation("com.discord4j:discord4j-core:3.3.3") {
        exclude(group = "org.reactivestreams", module = "reactive-streams")
    }
    implementation("com.vdurmont:emoji-java:5.1.1")
    ksp("org.chromagaming:ButtonProcessor:2.0-SNAPSHOT")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("26.3")
        jvmArgs("-Xms2G", "-Xmx2G")

        downloadPlugins {
            github("MilkBowl", "Vault", "1.7.3", "Vault.jar")
            github("EssentialsX", "Essentials", "2.22.0", "EssentialsX-2.22.0.jar")
        }

        pluginJars.from(runPlugins)
    }

    processResources {
        val props = mapOf("version" to version.toString().replace("-SNAPSHOT", ""))
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    test {
        useJUnitPlatform()
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

kotlin {
    jvmToolchain(25)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-XXLanguage:+UnnamedLocalVariables"))
}
