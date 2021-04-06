import org.bukkit.configuration.file.YamlConfiguration

import java.util.regex.Pattern
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

val saveConfigComments = TaskKey[Seq[File]]("saveConfigComments")
saveConfigComments := {
    val sv = (Compile / sources).value
    val cdataRegex = Pattern.compile("(?:def|val|var) (\\w+)(?::[^=]+)? = (?:(?:get(?:I)?Config)|(?:DPUtils.\\w+Data))") //Hack: DPUtils
    val clRegex = Pattern.compile("class (\\w+) extends (\\w+)")
    val objRegex = Pattern.compile("object (\\w+)")
    val config = new YamlConfiguration()
    for (file <- sv) {
        Using(Source.fromFile(file)) { src =>
            var clKey: String = null
            var comment: String = null
            var justCommented: Boolean = false
            for (line <- src.getLines) {
                val clMatcher = clRegex.matcher(line)
                if (clKey == null && clMatcher.find()) { //First occurrence
                    clKey = if (clMatcher.group(2).contains("Component"))
                        "components." + clMatcher.group(1)
                    else
                        "global"
                    /*println("Class: "+clKey)
                    println("Comment: "+comment)
                    println("Just commented: "+justCommented)
                    if (comment != null) { //Not checking justCommented because the object may have the comment and not extend anything
                        config.set(s"$clKey.generalDescriptionInsteadOfAConfig", comment.trim)
                        justCommented = false
                        comment = null
                        println("Found class comment for " + clKey)
                    }*/
                } else if (line.contains("/**")) {
                    comment = ""
                    justCommented = false
                } else if (line.contains("*/") && comment != null)
                    justCommented = true
                else if (comment != null) {
                    if (justCommented) {
                        if (clKey != null) {
                            val matcher = cdataRegex.matcher(line)
                            if (matcher.find())
                                config.set(s"$clKey.${matcher.group(1)}", comment.trim)
                        }
                        else {
                            val matcher = objRegex.matcher(line)
                            if (matcher.find())
                                config.set(s"${matcher.group(1)}.generalDescriptionInsteadOfAConfig", comment.trim)
                        }
                        justCommented = false
                        comment = null
                    }
                    else comment += line.replaceFirst("^\\s*\\*\\s+", "") + "\n"
                }
            }
            config.save("target/configHelp.yml")
        }.recover[Unit]({ case t => t.printStackTrace() })
    }
    Seq(file("target/configHelp.yml"))
}

resourceGenerators in Compile += saveConfigComments
