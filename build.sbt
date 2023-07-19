name := "Chroma-Discord"

version := "1.1"

scalaVersion := "2.13.11"

resolvers += "jitpack.io" at "https://jitpack.io"
resolvers += "paper-repo" at "https://papermc.io/repo/repository/maven-public/"
resolvers += Resolver.mavenLocal

// assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false)

libraryDependencies ++= Seq(
    "io.papermc.paper" % "paper-api" % "1.19-R0.1-SNAPSHOT" % Provided,

    "com.discord4j" % "discord4j-core" % "3.2.3",
    "com.vdurmont" % "emoji-java" % "5.1.1",
    "io.projectreactor" % "reactor-scala-extensions_2.13" % "0.8.0",

    "com.github.TBMCPlugins.ChromaCore" % "Chroma-Core" % "v2.0.0-SNAPSHOT" % Provided,
    "net.ess3" % "EssentialsX" % "2.17.1" % Provided,
    // https://mvnrepository.com/artifact/com.mojang/brigadier
    "com.mojang" % "brigadier" % "1.0.500" % "provided",
    // https://mvnrepository.com/artifact/net.kyori/examination-api
    "net.kyori" % "examination-api" % "1.3.0" % "provided",
    // https://mvnrepository.com/artifact/org.jetbrains.kotlin/kotlin-stdlib
    //"org.jetbrains.kotlin" % "kotlin-stdlib" % "1.8.20" % "provided",
    // https://mvnrepository.com/artifact/org.scalatest/scalatest
    "org.scalatest" %% "scalatest" % "3.2.16" % Test,
    // https://mvnrepository.com/artifact/com.github.seeseemelk/MockBukkit-v1.19
    "com.github.seeseemelk" % "MockBukkit-v1.19" % "2.29.0" % Test,
    "com.github.milkbowl" % "vault" % "master-SNAPSHOT" % Test
)

assembly / assemblyJarName := "Chroma-Discord.jar"
assembly / assemblyShadeRules := Seq(
    "io.netty", "com.fasterxml", "org.slf4j"
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
    Commenter.saveConfigComments((Compile / sources).value)
}

Compile / resourceGenerators += saveConfigComments
//scalacOptions ++= Seq("-release", "17", "--verbose")
scalacOptions ++= Seq("-release", "17")
//Test / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat
Test / fork := true // This changes the tests ran through sbt to work like IntelliJ tests, fixes mocking issues
/*excludeDependencies ++= Seq(
    ExclusionRule("org.bukkit"),
    ExclusionRule("io.papermc.paper"),
    ExclusionRule("com.destroystokyo")
)*/
excludeDependencies ++= Seq(
    ExclusionRule("net.milkbowl.vault", "VaultAPI")
)