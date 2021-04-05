lazy val commenter = RootProject(file("../commenter"))
lazy val root = (project in file(".")).dependsOn(commenter)
