import scala.io.Source
import scala.util.Using

name := "Chroma-Discord"

version := "1.1"

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

assemblyJarName in assembly := "Chroma-Discord.jar"
assemblyShadeRules in assembly := Seq(
    "io.netty", "com.fasterxml", "org.mockito", "org.slf4j"
).map { p =>
    ShadeRule.rename(s"$p.**" -> "btndvtm.dp.@0").inAll
}

assemblyMergeStrategy in assembly := {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
    // https://stackoverflow.com/a/55557287/457612
    case "module-info.class" => MergeStrategy.discard
    case x => (assemblyMergeStrategy in assembly).value(x)
}

val teszt = TaskKey[Unit]("teszt")
teszt := {
    val sv = (Compile / sources).value
    for (file <- sv) {
        Using(Source.fromFile(file)) { src =>
            for (line <- src.getLines) {
                if (line.contains("class"))
                    println(line + "")
            }
        }.recover[Unit]({ case t => t.printStackTrace() })
    }
}
