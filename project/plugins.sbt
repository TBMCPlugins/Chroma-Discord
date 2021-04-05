

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.15.0")

/*lazy val commenter = project.settings(Seq(
    name := "Chroma-Commenter",
    version := "1.0",
    organization := "com.github.TBMCPlugins"
))

lazy val root = (project in file(".")).dependsOn(commenter)*/

//addSbtPlugin("com.github.TBMCPlugins" % "Chroma-Commenter" % "1.0")
/*val Teszt = config("teszt").extend(Compile)
val teszt = TaskKey[Unit]("teszt")
teszt := target map { target => //teszt := { x.value }
    println("Teszt: " + target)
}*/
