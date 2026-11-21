package buttondevteam.discordplugin.mccommands

import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.commands.ConnectCommand
import buttondevteam.discordplugin.commands.VersionCommand
import buttondevteam.discordplugin.mcchat.MCChatUtils
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.mcchat.sender.DiscordSenderBase
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.discordplugin.util.DPState
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass
import buttondevteam.lib.chat.ICommand2MC
import buttondevteam.lib.chat.commands.MCCommandSettings
import buttondevteam.lib.player.ChromaGamerBase
import buttondevteam.lib.player.TBMCPlayer
import buttondevteam.lib.player.TBMCPlayerBase
import com.github.shynixn.mccoroutine.bukkit.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.lang.reflect.Method

@CommandClass(
    path = "discord", helpText = ["Discord", "This command allows performing Discord-related actions."]
)
class DiscordMCCommand : ICommand2MC() {
    @Command2.Subcommand
    fun accept(player: Player): Boolean {
        if (checkSafeMode(player)) return true
        val did = ConnectCommand.WaitingToConnect[player.uniqueId]
        if (did == null) {
            player.sendMessage("§cYou don't have a pending connection to Discord.")
            return true
        }
        val dp = ChromaGamerBase.getUser(did, DiscordUser::class.java)
        val mcp = TBMCPlayerBase.getPlayer(player.uniqueId, TBMCPlayer::class.java)
        dp.connectWith(mcp)
        ConnectCommand.WaitingToConnect.remove(player.uniqueId)
        MCChatUtils.UnconnectedSenders.remove(did) //Remove all unconnected, will be recreated where needed
        player.sendMessage("§bAccounts connected.")
        return true
    }

    @Command2.Subcommand
    fun decline(player: Player): Boolean {
        if (checkSafeMode(player)) return true
        val did = ConnectCommand.WaitingToConnect.remove(player.uniqueId)
        if (did == null) {
            player.sendMessage("§cYou don't have a pending connection to Discord.")
            return true
        }
        player.sendMessage("§bPending connection declined.")
        return true
    }

    @Command2.Subcommand(
        helpText = ["Reload Discord plugin", "Reloads the config. To apply some changes, you may need to also run /discord restart."]
    )
    @MCCommandSettings(permGroup = MCCommandSettings.MOD_GROUP)
    fun reload(sender: CommandSender): Unit =
        if (DiscordPlugin.plugin.tryReloadConfig()) sender.sendMessage("§bConfig reloaded.")
        else sender.sendMessage("§cFailed to reload config.")

    @Command2.Subcommand(
        helpText = ["Restart the plugin", "This command disables and then enables the plugin."]
    )
    @MCCommandSettings(permGroup = MCCommandSettings.MOD_GROUP)
    fun restart(sender: CommandSender) {
        val task: Runnable = task@{
            if (!DiscordPlugin.plugin.tryReloadConfig()) {
                sender.sendMessage("§cFailed to reload config so not restarting. Check the console.")
                return@task
            }
            MinecraftChatModule.state = DPState.RESTARTING_PLUGIN //Reset in MinecraftChatModule
            sender.sendMessage("§bDisabling DiscordPlugin...")
            Bukkit.getPluginManager().disablePlugin(DiscordPlugin.plugin)
            if (sender !is DiscordSenderBase) { //Sending to Discord errors
                sender.sendMessage("§bEnabling DiscordPlugin...")
            }
            Bukkit.getPluginManager().enablePlugin(DiscordPlugin.plugin)
            if (sender !is DiscordSenderBase) sender.sendMessage("§bRestart finished!")
        }

        if (Bukkit.getName() != "Paper") {
            plugin.logger.warning("Async plugin events are not supported by the server, running on main thread")
            Bukkit.getScheduler().runTask(DiscordPlugin.plugin, task)
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, task)
        }
    }

    @Command2.Subcommand(
        helpText = ["Version command", "Prints the plugin version"]
    )
    fun version(sender: CommandSender) {
        sender.sendMessage(*VersionCommand.getVersion())
    }

    @Command2.Subcommand(
        helpText = ["Invite", "Shows an invite link to the server"]
    )
    fun invite(sender: CommandSender) {
        if (checkSafeMode(sender)) {
            return
        }
        val invi: String = DiscordPlugin.plugin.inviteLink
        if (invi.isNotEmpty()) {
            sender.sendMessage("§bInvite link: $invi")
            return
        }
        plugin.launch {
            try {
                val invite = DiscordPlugin.plugin.mainServer.invites.awaitFirstOrNull()
                if (invite == null) {
                    sender.sendMessage("§cNo invites found for the server.")
                } else {
                    sender.sendMessage("§bInvite link: https://discord.gg/" + invite.code)
                }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    ensureActive()
                }
                e.printStackTrace()
                sender.sendMessage("§cThe invite link is not set and the bot has no permission to get it.")
            }
        }
    }

    override fun getHelpText(method: Method, ann: Command2.Subcommand): Array<String> {
        return when (method.name) {
            "accept" ->
                arrayOf(
                    "Accept Discord connection",
                    "Accept a pending connection between your Discord and Minecraft account.",
                    "To start the connection process, do §b/connect <MCname>§r in the " + DPUtils.botmention() + " channel on Discord"
                )

            "decline" ->
                arrayOf(
                    "Decline Discord connection",
                    "Decline a pending connection between your Discord and Minecraft account.",
                    "To start the connection process, do §b/connect <MCname>§r in the " + DPUtils.botmention() + " channel on Discord"
                )

            else ->
                super.getHelpText(method, ann)
        }
    }

    private fun checkSafeMode(sender: CommandSender): Boolean {
        return if (DiscordPlugin.SafeMode) {
            sender.sendMessage("§cThe plugin isn't initialized. Check console for details.")
            true
        } else false
    }
}