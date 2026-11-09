package buttondevteam.discordplugin.mccommands

import buttondevteam.discordplugin.DPUtils.FluxExtensions
import buttondevteam.discordplugin.commands.{ConnectCommand, VersionCommand}
import buttondevteam.discordplugin.mcchat.sender.{DiscordSenderBase, DiscordUser}
import buttondevteam.discordplugin.mcchat.{MCChatUtils, MinecraftChatModule}
import buttondevteam.discordplugin.util.DPState
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.chat.commands.MCCommandSettings
import buttondevteam.lib.chat.{Command2, CommandClass, ICommand2MC}
import buttondevteam.lib.player.{ChromaGamerBase, TBMCPlayer, TBMCPlayerBase}
import discord4j.core.`object`.ExtendedInvite
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import reactor.core.publisher.Mono

import java.lang.reflect.Method

@CommandClass(path = "discord", helpText = Array(
    "Discord",
    "This command allows performing Discord-related actions."
)) class DiscordMCCommand extends ICommand2MC {
    @Command2.Subcommand
    def accept(player: Player): Boolean = {
        if (checkSafeMode(player)) return true
        val did = ConnectCommand.WaitingToConnect.get(player.getName)
        if (did == null) {
            player.sendMessage("§cYou don't have a pending connection to Discord.")
            return true
        }
        val dp = ChromaGamerBase.getUser(did, classOf[DiscordUser])
        val mcp = TBMCPlayerBase.getPlayer(player.getUniqueId, classOf[TBMCPlayer])
        dp.connectWith(mcp)
        ConnectCommand.WaitingToConnect.remove(player.getName)
        MCChatUtils.UnconnectedSenders.remove(did) //Remove all unconnected, will be recreated where needed
        player.sendMessage("§bAccounts connected.")
        true
    }

    @Command2.Subcommand
    def decline(player: Player): Boolean = {
        if (checkSafeMode(player)) return true
        val did = ConnectCommand.WaitingToConnect.remove(player.getName)
        if (did == null) {
            player.sendMessage("§cYou don't have a pending connection to Discord.")
            return true
        }
        player.sendMessage("§bPending connection declined.")
        true
    }

    @Command2.Subcommand(helpText = Array(
        "Reload Discord plugin",
        "Reloads the config. To apply some changes, you may need to also run /discord restart."
    ))
    @MCCommandSettings(permGroup = MCCommandSettings.MOD_GROUP)
    def reload(sender: CommandSender): Unit =
        if (DiscordPlugin.plugin.tryReloadConfig) sender.sendMessage("§bConfig reloaded.")
        else sender.sendMessage("§cFailed to reload config.")

    @Command2.Subcommand(helpText = Array(
        "Restart the plugin", //
        "This command disables and then enables the plugin." //
    ))
    @MCCommandSettings(permGroup = MCCommandSettings.MOD_GROUP)
    def restart(sender: CommandSender): Unit = {
        val task: Runnable = () => {
            def foo(): Unit = {
                if (!DiscordPlugin.plugin.tryReloadConfig) {
                    sender.sendMessage("§cFailed to reload config so not restarting. Check the console.")
                    return ()
                }
                MinecraftChatModule.state = DPState.RESTARTING_PLUGIN //Reset in MinecraftChatModule
                sender.sendMessage("§bDisabling DiscordPlugin...")
                Bukkit.getPluginManager.disablePlugin(DiscordPlugin.plugin)
                if (!sender.isInstanceOf[DiscordSenderBase]) { //Sending to Discord errors
                    sender.sendMessage("§bEnabling DiscordPlugin...")
                }
                Bukkit.getPluginManager.enablePlugin(DiscordPlugin.plugin)
                if (!sender.isInstanceOf[DiscordSenderBase]) sender.sendMessage("§bRestart finished!")
            }

            foo()
        }

        if (!(Bukkit.getName == "Paper")) {
            getPlugin.getLogger.warning("Async plugin events are not supported by the server, running on main thread")
            Bukkit.getScheduler.runTask(DiscordPlugin.plugin, task)
        }
        else {
            Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, task)
        }
    }

    @Command2.Subcommand(helpText = Array(
        "Version command",
        "Prints the plugin version"))
    def version(sender: CommandSender): Unit = {
        sender.sendMessage(VersionCommand.getVersion: _*)
    }

    @Command2.Subcommand(helpText = Array(
        "Invite",
        "Shows an invite link to the server"
    ))
    def invite(sender: CommandSender): Unit = {
        if (checkSafeMode(sender)) {
            return ()
        }
        val invi: String = DiscordPlugin.plugin.inviteLink.get
        if (invi.nonEmpty) {
            sender.sendMessage("§bInvite link: " + invi)
            return ()
        }
        DiscordPlugin.mainServer.getInvites.^^().take(1)
            .switchIfEmpty(Mono.fromRunnable(() => sender.sendMessage("§cNo invites found for the server.")))
            .subscribe((inv: ExtendedInvite) => sender.sendMessage("§bInvite link: https://discord.gg/" + inv.getCode), Some(_ => sender.sendMessage("§cThe invite link is not set and the bot has no permission to get it.")))
    }

    override def getHelpText(method: Method, ann: Command2.Subcommand): Array[String] = {
        method.getName match {
            case "accept" =>
                Array[String]("Accept Discord connection", "Accept a pending connection between your Discord and Minecraft account.", "To start the connection process, do §b/connect <MCname>§r in the " + DPUtils.botmention + " channel on Discord")
            case "decline" =>
                Array[String]("Decline Discord connection", "Decline a pending connection between your Discord and Minecraft account.", "To start the connection process, do §b/connect <MCname>§r in the " + DPUtils.botmention + " channel on Discord")
            case _ =>
                super.getHelpText(method, ann)
        }
    }

    private def checkSafeMode(sender: CommandSender): Boolean = {
        if (DiscordPlugin.SafeMode) {
            sender.sendMessage("§cThe plugin isn't initialized. Check console for details.")
            true
        }
        else false
    }
}