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

val getConfigComments = TaskKey[Unit]("getConfigComments")
getConfigComments := {
    val sv = (Compile / sources).value
    //val cdataRegex = Pattern.compile("(?:def|val|var) \\w*ConfigData\\w*(?:\\[\\w+])? (\\w+)")
    val cdataRegex = Pattern.compile("(?:def|val|var) (\\w+)(?::[^=]+)? = get(?:I)?Config")
    val clRegex = Pattern.compile("class (\\w+) extends (\\w+)")
    for (file <- sv) {
        Using(Source.fromFile(file)) { src =>
            var pkg: String = null
            var cl: String = null
            var comment: String = null
            var justCommented: Boolean = false
            var isComponent: Boolean = false
            for (line <- src.getLines) {
                val clMatcher = clRegex.matcher(line)
                if (line.startsWith("package")) {
                    pkg = line.substring("package ".length)
                    //println("Found package: " + pkg)
                } else if (line.contains("class") && pkg != null && cl == null && clMatcher.find()) { //First occurrence
                    //cl = line.substring(line.indexOf("class") + "class ".length)
                    cl = clMatcher.group(1)
                    isComponent = clMatcher.group(2).contains("Component")
                    //println("Found class: " + cl)
                } else if (line.contains("/**") && cl != null) {
                    comment = ""
                    justCommented = false
                    //println("Found comment start")
                } else if (line.contains("*/") && comment != null) {
                    justCommented = true
                    //println("Found comment end")
                } else if (comment != null) {
                    if (justCommented) {
                        //println("Just commented")
                        //println(s"line: $line")
                        val matcher = cdataRegex.matcher(line)
                        if (matcher.find())
                            println(s"$pkg.$cl.${matcher.group(1)} comment:\n" + comment)
                        justCommented = false
                        comment = null
                    }
                    else {
                        comment += line.replaceFirst("^\\s*\\*\\s+", "")
                        //println("Adding to comment")
                    }
                }
            }
        }.recover[Unit]({ case t => t.printStackTrace() })
    }
}
