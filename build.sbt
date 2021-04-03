name := "Chroma-Discord"

version := "0.1"

scalaVersion := "2.13.5"

resolvers += "spigot-repo" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
resolvers += "jitpack.io" at "https://jitpack.io"
resolvers += Resolver.mavenLocal

libraryDependencies ++= Seq(
    "org.spigotmc" % "spigot-api" % "1.12.2-R0.1-SNAPSHOT" % Provided,
    "org.spigotmc" % "spigot" % "1.12.2-R0.1-SNAPSHOT" % Provided,
    "org.spigotmc." % "spigot" % "1.14.4-R0.1-SNAPSHOT" % Provided,
    "com.destroystokyo.paper" % "paper" % "1.16.3-R0.1-SNAPSHOT" % Provided,

    "com.discord4j" % "discord4j-core" % "3.1.4",
    "org.slf4j" % "slf4j-jdk14" % "1.7.21",
    "com.vdurmont" % "emoji-java" % "4.0.0",
    "org.mockito" % "mockito-core" % "3.5.13",
    "io.projectreactor" %% "reactor-scala-extensions" % "0.7.0",

    "com.github.TBMCPlugins.ChromaCore" % "Chroma-Core" % "v1.0.0" % Provided,
    "net.ess3" % "EssentialsX" % "2.17.1" % Provided,
    "com.github.lucko.LuckPerms" % "bukkit" % "master-SNAPSHOT" % Provided,
)


/*val myAssemblySettings = inTask(assembly)(
    Seq(
        assemblyShadeRules := libraryDependencies.value.filter(!_.configurations.exists(_ contains "provided"))
            .map { _.organization }
            .map { p =>
            ShadeRule.rename(s"$p.**" -> "btndvtm.dp.@0").inAll
        },
        assemblyMergeStrategy := {
            case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
            // https://stackoverflow.com/a/55557287/457612
            case "module-info.class" => MergeStrategy.discard
            case x                   => assemblyMergeStrategy.value(x)
        },
        /*shadeResourceTransformers ++= Seq(
            Rename(
                "libnetty_tcnative_linux_x86_64.so"   -> "libcom_couchbase_client_core_deps_netty_tcnative_linux_x86_64.so",
                "libnetty_tcnative_osx_x86_64.jnilib" -> "libcom_couchbase_client_core_deps_netty_tcnative_osx_x86_64.jnilib",
                "netty_tcnative_windows_x86_64.dll"   -> "com_couchbase_client_core_deps_netty_tcnative_windows_x86_64.dll"
            ).inDir("META-INF/native"),
            Discard(
                "com.fasterxml.jackson.core.JsonFactory",
                "com.fasterxml.jackson.core.ObjectCodec",
                "com.fasterxml.jackson.databind.Module"
            ).inDir("META-INF/services")
        )*/
    )
)*/

assemblyJarName in assembly := "Chroma-Discord.jar"
//assemblyShadeRules in assembly := libraryDependencies.value.filter(!_.configurations.exists(_ contains "provided"))
assemblyShadeRules in assembly := Seq(
    "io.netty", "com.fasterxml", "org.mockito", "org.slf4j"
).map { p =>
    ShadeRule.rename(s"$p.**" -> "btndvtm.dp.@0").inAll
}

//logLevel in assembly := Level.Debug

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
    // https://stackoverflow.com/a/55557287/457612
    case "module-info.class" => MergeStrategy.discard
    case x => (assemblyMergeStrategy in assembly).value(x)
}

//lazy val `Chroma-Discord` = project.settings(myAssemblySettings)
