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
    val subRegex = Pattern.compile("def `?(\\w+)`?\\((?:((?:\\w|\\d)+): ((?:\\w|[\\[\\].]|\\d)+),?\\s*)+\\)")
    val config = new YamlConfiguration()

    def getConfigComments(line: String, clKey: String, comment: String, justCommented: Boolean): (String, String, Boolean) = {
        val clMatcher = clRegex.matcher(line)
        if (clKey == null && clMatcher.find()) { //First occurrence
            (if (clMatcher.group(2).contains("Component"))
                "components." + clMatcher.group(1)
            else "global", comment, justCommented)
        } else if (line.contains("/**")) {
            (clKey, "", false)
        } else if (line.contains("*/") && comment != null)
            (clKey, comment, true)
        else if (comment != null) {
            if (justCommented) {
                if (clKey != null) {
                    val matcher = cdataRegex.matcher(line)
                    if (matcher.find())
                        config.set(s"$clKey.${matcher.group(1)}", comment.trim)
                }
                else {
                    val matcher = objRegex.matcher(line)
                    if (matcher.find()) {
                        val clName = matcher.group(1)
                        val compKey = if (clName.contains("Module")) s"component.$clName"
                        else if (clName.contains("Plugin")) "global"
                        else null
                        if (compKey != null)
                            config.set(s"${compKey}.generalDescriptionInsteadOfAConfig", comment.trim)
                    }
                }
                (clKey, null, false)
            }
            else (clKey, comment + line.replaceFirst("^\\s*\\*\\s+", "") + "\n", justCommented)
        }
        else (clKey, comment, justCommented)
    }

    for (file <- sv) {
        Using(Source.fromFile(file)) { src =>
            var clKey: String = null
            var comment: String = null
            var justCommented: Boolean = false

            var subCommand = false
            var pkg: String = null
            var clName: String = null
            for (line <- src.getLines) {
                val (clk, c, jc) = getConfigComments(line, clKey, comment, justCommented)
                clKey = clk; comment = c; justCommented = jc

                val objMatcher = objRegex.matcher(line)
                val clMatcher = clRegex.matcher(line)
                if (pkg == null && line.startsWith("package "))
                    pkg = line.substring("package ".length)
                /*else if (clName == null && (line.contains("object") || line.contains("class"))
                && !line.contains("import")) {
                    val i = line.indexOf("class")
                    val j = if (i == -1) line.indexOf("object") + "object ".length else i + "class ".length
                    clName = line.substring(j)
                }*/
                else if (clName == null && objMatcher.find())
                    clName = objMatcher.group(1)
                else if (clName == null && clMatcher.find())
                    clName = clMatcher.group(1)
                val subMatcher = subRegex.matcher(line)
                val sub = line.contains("@Subcommand") || line.contains("@Command2.Subcommand")
                if (subCommand || sub) //This line or the previous one had the annotation
                    if (subMatcher.find()) {
                        val groups = (2 to subMatcher.groupCount()).map(subMatcher.group)
                        val pairs = for (i <- groups.indices by 2) yield (groups(i), groups(i + 1))
                        val mname = subMatcher.group(1)
                        print(s"$pkg.$clName.$mname(")
                        for ((name, ty) <- pairs) print(s"$name: $ty, ")
                        println(")")
                    }
                subCommand = sub
            }
            config.save("target/configHelp.yml")
        }.recover[Unit]({ case t => t.printStackTrace() })
    }
    Seq(file("target/configHelp.yml"))
}

resourceGenerators in Compile += saveConfigComments
