import buttondevteam.buttonproc.HasConfig
import buttondevteam.lib.architecture.ConfigData
import org.reflections.Reflections

import scala.jdk.javaapi.CollectionConverters.asScala

object Commenter extends App {
    val ref = new Reflections("buttondevteam.discordplugin")
    val types = asScala(ref.getTypesAnnotatedWith(classOf[HasConfig], true))
    for (ty <- types) {
        val path = if (ty.getAnnotation(classOf[HasConfig]).global()) "global"
        else s"components.${ty.getSimpleName}"
        val cdcl = classOf[ConfigData[_]]
        ty.getDeclaredMethods.filter(m => cdcl.isAssignableFrom(m.getReturnType))
            .concat(ty.getDeclaredFields.filter(f => cdcl.isAssignableFrom(f.getType)))
            .map(path + "." + _.getName)
    }
}
