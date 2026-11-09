package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.{CommandClass, ICommand2}

abstract class ICommand2DC() extends ICommand2[Command2DCSender](DiscordPlugin.plugin.manager) {
    final private var modOnly = false

    {
        val ann: CommandClass = getClass.getAnnotation(classOf[CommandClass])
        if (ann == null) modOnly = false
        else modOnly = ann.modOnly
    }

    def isModOnly: Boolean = this.modOnly
}