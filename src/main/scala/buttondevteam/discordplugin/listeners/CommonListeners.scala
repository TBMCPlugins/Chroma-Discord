package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.fun.FunModule
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.Timings
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.{MessageChannel, PrivateChannel}
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.event.domain.interaction.{ChatInputInteractionEvent, MessageInteractionEvent}
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.role.{RoleCreateEvent, RoleDeleteEvent, RoleUpdateEvent}
import reactor.core.Disposable
import reactor.core.publisher.Mono
import reactor.core.scala.publisher.{SFlux, SMono}

object CommonListeners {
    val timings = new Timings

    def register(dispatcher: EventDispatcher): Unit = {
        dispatcher.on(classOf[MessageCreateEvent]).flatMap((event: MessageCreateEvent) => {
            SMono.just(event.getMessage).filter(_ => !DiscordPlugin.SafeMode)
                .filter(message => message.getAuthor.filter(!_.isBot).isPresent)
                .filter(message => !FunModule.executeMemes(message))
                .filterWhen(message => {
                    Option(Component.getComponents.get(classOf[MinecraftChatModule])).filter(_.isEnabled)
                        .map(_.asInstanceOf[MinecraftChatModule].getListener.handleDiscord(event))
                        .getOrElse(SMono.just(true)) //Wasn't handled, continue
                })
        }).onErrorContinue((err, _) => TBMCCoreAPI.SendException("An error occured while handling a message!", err, DiscordPlugin.plugin)).subscribe()
        dispatcher.on(classOf[PresenceUpdateEvent]).subscribe((event: PresenceUpdateEvent) => {
            if (!DiscordPlugin.SafeMode)
                FunModule.handleFullHouse(event)
        })
        SFlux(dispatcher.on(classOf[RoleCreateEvent])).subscribe(GameRoleModule.handleRoleEvent)
        SFlux(dispatcher.on(classOf[RoleDeleteEvent])).subscribe(GameRoleModule.handleRoleEvent)
        SFlux(dispatcher.on(classOf[RoleUpdateEvent])).subscribe(GameRoleModule.handleRoleEvent)
        SFlux(dispatcher.on(classOf[ChatInputInteractionEvent], event => {
            if(event.getCommandName() eq "help")
                event.reply("Hello there")
            else
                Mono.empty()
        })).subscribe()
    }

    var debug = false

    def debug(debug: String): Unit = if (CommonListeners.debug) { //Debug
        DPUtils.getLogger.info(debug)
    }
}