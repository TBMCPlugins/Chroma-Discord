//lazy val commenter = RootProject(file("../commenter"))
//lazy val root = (project in file(".")).dependsOn(commenter)

resolvers += Resolver.mavenLocal
resolvers += "paper-repo" at "https://papermc.io/repo/repository/maven-public/"

libraryDependencies += "io.papermc.paper" % "paper-api" % "1.19.4-R0.1-SNAPSHOT" % Compile
