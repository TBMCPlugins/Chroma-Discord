lazy val commenter = project.settings(Seq(
    name := "Chroma-Commenter",
    version := "1.0",
    organization := "com.github.TBMCPlugins",

    resolvers += Resolver.mavenLocal,

    libraryDependencies += "org.reflections" % "reflections" % "0.9.12",
    libraryDependencies += "com.github.TBMCPlugins.ChromaCore" % "ButtonProcessor" % "master-SNAPSHOT"
))
