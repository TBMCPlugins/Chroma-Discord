

object Commenter extends App {
    val ref = new Reflections("buttondevteam.discordplugin")
    val types: Set[Class[_]] = ref.getTypesAnnotatedWith(HasConfig, true).asScala
    for (ty <- types) {
        ty
        .
    }
}
