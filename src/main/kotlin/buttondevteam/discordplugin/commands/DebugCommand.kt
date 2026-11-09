package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.listeners.CommonListeners
import discord4j.common.util.Snowflake
import reactor.core.scala.publisher.SMono

@CommandClass(helpText = arrayOf("Switches debug mode."))
class DebugCommand : ICommand2DC {
    @Command2.Subcommand
    override fun def(sender: Command2DCSender): Boolean = {
        SMono.justOrEmpty(sender.authorAsMember.orNull)
            .switchIfEmpty(Option(sender.author) //Support DMs
                .map((u: User) -> u.asMember(DiscordPlugin.mainServer.getId).^^()).getOrElse(SMono.empty[Member]))
            .flatMap((m: Member) -> DiscordPlugin.plugin.modRole.get
                .map(mr -> m.getRoleIds.stream.anyMatch((r: Snowflake) -> r == mr.getId))
                .switchIfEmpty(SMono.fromCallable(() -> DiscordPlugin.mainServer.getOwnerId.asLong == m.getId.asLong)))
            .onErrorResume(_ -> SMono.just(false)) //Role not found
            .subscribe(success -> {
                if (success) {
                    CommonListeners.debug = !CommonListeners.debug
                    sender.sendMessage("debug " + (if (CommonListeners.debug) "enabled" else "disabled"))
                } else
                    sender.sendMessage("you need to be a moderator to use this command.")
            })
        true
    }
}