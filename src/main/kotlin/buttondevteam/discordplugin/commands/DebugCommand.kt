package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.listeners.CommonListeners
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass
import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.reactive.awaitFirstOrNull

@CommandClass(helpText = ["Switches debug mode."])
class DebugCommand : ICommand2DC() {
    @Command2.Subcommand
    override fun def(sender: Command2DCSender): Boolean {
        DiscordPlugin.plugin.launch {
            val member = sender.authorAsMember
            // Support DMs
                ?: sender.author.asMember(DiscordPlugin.plugin.mainServer!!.id).awaitFirstOrNull()
                ?: return@launch
            val isMod = member.roleIds.any { it.asLong() == DiscordPlugin.plugin.modRole.id.asLong() }
            val isOwner = DiscordPlugin.plugin.mainServer!!.ownerId.asLong() == member.id.asLong()
            if (isMod || isOwner) {
                CommonListeners.debug = !CommonListeners.debug
                sender.sendMessage("debug " + (if (CommonListeners.debug) "enabled" else "disabled"))
            } else {
                sender.sendMessage("you need to be a moderator to use this command.")
            }
        }
        return true
    }
}