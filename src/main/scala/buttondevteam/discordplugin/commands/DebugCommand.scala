package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.listeners.CommonListeners
import buttondevteam.lib.chat.{Command2, CommandClass}
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.{Member, User}
import reactor.core.publisher.Mono
import scala.jdk.OptionConverters._

@CommandClass(helpText = Array("Switches debug mode."))
class DebugCommand extends ICommand2DC {
    @Command2.Subcommand
    override def `def`(sender: Command2DCSender): Boolean = {
        Mono.justOrEmpty(sender.authorAsMember.orNull)
            .switchIfEmpty(Option(sender.author) //Support DMs
                .map((u: User) => u.asMember(DiscordPlugin.mainServer.getId)).toJava.orElse(Mono.empty[Member]))
            .flatMap((m: Member) => DiscordPlugin.plugin.modRole.get
                .map(mr => m.getRoleIds.stream.anyMatch((r: Snowflake) => r == mr.getId))
                .switchIfEmpty(Mono.fromCallable(() => DiscordPlugin.mainServer.getOwnerId.asLong == m.getId.asLong)))
            .onErrorResume(_ => Mono.just(false)) //Role not found
            .subscribe(success => {
                if (success) {
                    CommonListeners.debug = !CommonListeners.debug
                    sender.sendMessage("debug " + (if (CommonListeners.debug) "enabled" else "disabled"))
                } else
                    sender.sendMessage("you need to be a moderator to use this command.")
            })
        true
    }
}