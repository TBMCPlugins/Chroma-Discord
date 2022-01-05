package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.listeners.CommonListeners
import buttondevteam.lib.chat.{Command2, CommandClass}
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.{Member, User}
import reactor.core.scala.publisher.SMono

@CommandClass(helpText = Array("Switches debug mode."))
class DebugCommand extends ICommand2DC {
    @Command2.Subcommand
    override def `def`(sender: Command2DCSender): Boolean = {
        SMono(sender.getMessage.getAuthorAsMember)
            .switchIfEmpty(Option(sender.getMessage.getAuthor.orElse(null)) //Support DMs
                .map((u: User) => SMono(u.asMember(DiscordPlugin.mainServer.getId))).getOrElse(SMono.empty))
            .flatMap((m: Member) => DiscordPlugin.plugin.modRole.get
                .map(mr => m.getRoleIds.stream.anyMatch((r: Snowflake) => r == mr.getId))
                .switchIfEmpty(SMono.fromCallable(() => DiscordPlugin.mainServer.getOwnerId.asLong == m.getId.asLong)))
            .onErrorResume(_ => SMono.just(false)) //Role not found
            .subscribe(success => {
                if (success) {
                    CommonListeners.debug = !CommonListeners.debug;
                    sender.sendMessage("debug " + (if (CommonListeners.debug) "enabled" else "disabled"))
                } else
                    sender.sendMessage("you need to be a moderator to use this command.")
            })
        true
    }
}