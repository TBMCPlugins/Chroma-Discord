import org.bukkit.configuration.file.YamlConfiguration

import java.util.regex.Pattern
import scala.io.Source
import scala.util.Using

name := "Chroma-Discord"

version := "1.1"

scalaVersion := "3.3.0"

resolvers += "spigot-repo" at "https://hub.spigotmc.org/nexus/content/repositories/snapshots/"
resolvers += "jitpack.io" at "https://jitpack.io"
resolvers += "paper-repo" at "https://papermc.io/repo/repository/maven-public/"
resolvers += Resolver.mavenLocal

// assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false)

libraryDependencies ++= Seq(
    "io.papermc.paper" % "paper-api" % "1.19.4-R0.1-SNAPSHOT" % Provided,

    "com.discord4j" % "discord4j-core" % "3.2.3",
    "com.vdurmont" % "emoji-java" % "5.1.1",
    "org.mockito" % "mockito-core" % "5.2.0",

    "com.github.TBMCPlugins.ChromaCore" % "Chroma-Core" % "v2.0.0-SNAPSHOT" % Provided,
    "net.ess3" % "EssentialsX" % "2.17.1" % Provided,
    "net.luckperms" % "api" % "5.4" % Provided,
    // https://mvnrepository.com/artifact/com.mojang/brigadier
    "com.mojang" % "brigadier" % "1.0.500" % "provided",
    // https://mvnrepository.com/artifact/net.kyori/examination-api
    "net.kyori" % "examination-api" % "1.3.0" % "provided",
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    "org.jetbrains.kotlin" % "kotlin-stdlib" % "1.8.20" % "provided"
)

assembly / assemblyJarName := "Chroma-Discord.jar"
assembly / assemblyShadeRules := Seq(
    "io.netty", "com.fasterxml", "org.mockito", "org.slf4j"
).map { p =>
    ShadeRule.rename(s"$p.**" -> "btndvtm.dp.@0").inAll
}

assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.concat
    // https://stackoverflow.com/a/55557287/457612
    case "module-info.class" => MergeStrategy.discard
    case "META-INF/versions/9/module-info.class" => MergeStrategy.discard
    case x => (assembly / assemblyMergeStrategy).value(x)
}

val saveConfigComments = TaskKey[Seq[File]]("saveConfigComments")
saveConfigComments := {
    val sv = (Compile / sources).value
    val cdataRegex = Pattern.compile("(?:def|val|var) (\\w+)(?::[^=]+)? = (?:getI?Config|DPUtils.\\w+Data)") //Hack: DPUtils
    val clRegex = Pattern.compile("class (\\w+).* extends ((?:\\w|\\d)+)")
    val objRegex = Pattern.compile("object (\\w+)")
    val subRegex = Pattern.compile("def `?(\\w+)`?")
    val subParamRegex = Pattern.compile("((?:\\w|\\d)+): ((?:\\w|\\d)+)")
    val configConfig = new YamlConfiguration()
    val commandConfig = new YamlConfiguration()

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
                        configConfig.set(s"$clKey.${matcher.group(1)}", comment.trim)
                }
                else {
                    val matcher = objRegex.matcher(line)
                    if (matcher.find()) {
                        val clName = matcher.group(1)
                        val compKey = if (clName.contains("Module")) s"component.$clName"
                        else if (clName.contains("Plugin")) "global"
                        else null
                        if (compKey != null)
                            configConfig.set(s"$compKey.generalDescriptionInsteadOfAConfig", comment.trim)
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
                else if (clName == null && objMatcher.find())
                    clName = objMatcher.group(1)
                else if (clName == null && clMatcher.find())
                    clName = clMatcher.group(1)
                val subMatcher = subRegex.matcher(line)
                val subParamMatcher = subParamRegex.matcher(line)
                val sub = line.contains("@Subcommand") || line.contains("@Command2.Subcommand")
                if (sub) subCommand = true
                else if (line.contains("}")) subCommand = false
                if (subCommand && subMatcher.find()) {
                    /*val groups = (2 to subMatcher.groupCount()).map(subMatcher.group)
                    val pairs = for (i <- groups.indices by 2) yield (groups(i), groups(i + 1))*/
                    val mname = subMatcher.group(1)
                    val params = Iterator.continually(()).takeWhile(_ => subParamMatcher.find())
                        .map(_ => subParamMatcher.group(1)).drop(1)
                    val section = commandConfig.createSection(s"$pkg.$clName.$mname")
                    section.set("method", s"$mname()")
                    section.set("params", params.mkString(" "))
                    subCommand = false
                }
            }
            configConfig.save("target/configHelp.yml")
            commandConfig.save("target/commands.yml")
        }.recover[Unit]({ case t => t.printStackTrace() })
    }
    Seq(file("target/configHelp.yml"), file("target/commands.yml"))
}

Compile / resourceGenerators += saveConfigComments
//scalacOptions ++= Seq("-release", "17", "--verbose")
scalacOptions ++= Seq("-release", "17")
