package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.listeners.CommonListeners
import buttondevteam.lib.chat.{Command2, CommandClass}
import discord4j.core.`object`.entity.{Member, User}
import reactor.core.publisher.Mono

@CommandClass(helpText = Array(Array("Switches debug mode.")))
class DebugCommand extends ICommand2DC {
    @Command2.Subcommand
    override def `def`(sender: Command2DCSender): Boolean = {
        sender.getMessage.getAuthorAsMember.switchIfEmpty(sender.getMessage.getAuthor.map //Support DMs
        ((u: User) => u.asMember(DiscordPlugin.mainServer.getId)).orElse(Mono.empty)).flatMap((m: Member) => DiscordPlugin.plugin.modRole.get.map((mr) => m.getRoleIds.stream.anyMatch((r: Snowflake) => r == mr.getId)).switchIfEmpty(Mono.fromSupplier(() => DiscordPlugin.mainServer.getOwnerId.asLong eq m.getId.asLong)))
            .onErrorReturn(false) //Role not found
            .subscribe((success: Any) => {
                def foo(success: Any) = {
                    if (success) sender.sendMessage("debug " + (if (CommonListeners.debug) "enabled"
                    else "disabled"))
                    else sender.sendMessage("you need to be a moderator to use this command.")
                }

                foo(success)
            })
        true
    }
}