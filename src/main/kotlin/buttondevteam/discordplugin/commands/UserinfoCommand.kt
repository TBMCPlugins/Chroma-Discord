package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass
import buttondevteam.lib.player.ChromaGamerBase
import buttondevteam.lib.player.ChromaGamerBase.InfoTarget
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull

@CommandClass(helpText = [
    "User information",
    "Shows some information about users, from Discord, from Minecraft or from Reddit if they have these accounts connected.",
    "If used without args, shows your info."
])
class UserinfoCommand : ICommand2DC() {
    @Command2.Subcommand
    fun def(sender: Command2DCSender, @Command2.OptionalArg @Command2.TextArg user: String? = null): Boolean {
        var target: User? = null
        if (user.isNullOrEmpty()) target = sender.author
        else { // TODO: Mention option
        }
        if (target == null) {
            sender.sendMessage("An error occurred.")
            return true
        }
        val dp = ChromaGamerBase.getUser(target.id.asString(), DiscordUser::class.java)
        val uinfo = StringBuilder("User info for ").append(target.username).append(":\n")
        uinfo.append(dp.getInfo(InfoTarget.Discord))
        sender.sendMessage(uinfo.toString())
        return true
    }

    private suspend fun getUsers(message: Message, args: String): List<User> {
        val guild = message.guild.awaitFirstOrNull()
        return guild?.members?.filter { it.username.contentEquals(args, true) }?.asFlow()?.toList()
            ?: //Private channel
            DiscordPlugin.dc.users.filter { u -> u.username.contentEquals(args, true) }.asFlow().toList()
    }
}