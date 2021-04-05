name := "Chroma-Commenter"
version := "1.0"
organization := "com.github.TBMCPlugins"
scalaVersion := "2.13.4"

resolvers += Resolver.mavenLocal

libraryDependencies += "org.reflections" % "reflections" % "0.9.12"
libraryDependencies += "com.github.TBMCPlugins.ChromaCore" % "ButtonProcessor" % "master-SNAPSHOT"
libraryDependencies += "com.github.TBMCPlugins.ChromaCore" % "Chroma-Core" % "v1.0.0"
