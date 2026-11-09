package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.CommandClass
import buttondevteam.lib.chat.ICommand2

abstract class ICommand2DC : ICommand2<Command2DCSender>(DiscordPlugin.plugin!!.manager) {
    private var modOnly = false

    init {
        val ann = javaClass.getAnnotation(CommandClass::class.java)
        modOnly = ann?.modOnly ?: false
    }

    fun isModOnly(): Boolean = this.modOnly
}