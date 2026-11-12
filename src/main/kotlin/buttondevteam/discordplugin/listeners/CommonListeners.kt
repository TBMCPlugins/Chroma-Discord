package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.fun.FunModule
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.Timings
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import reactor.core.scala.publisher.SMono

object CommonListeners {
    val timings = Timings

    fun register(dispatcher: EventDispatcher): Unit = {
        dispatcher.on(MessageCreateEvent::class.java).flatMap((event: MessageCreateEvent) -> {
            SMono.just(event.getMessage).filter(_ -> !DiscordPlugin.SafeMode)
                .filter(message -> message.getAuthor.filter(!_.isBot).isPresent)
                .filter(message -> !FunModule.executeMemes(message))
                .filterWhen(message -> {
                    Option(Component.getComponents.get(MinecraftChatModule::class.java)).filter(_.isEnabled)
                        .map(_.asInstanceOf[MinecraftChatModule].getListener.handleDiscord(event))
                        .getOrElse(SMono.just(true)) //Wasn't handled, continue
                })
        }).onErrorContinue((err, _) -> TBMCCoreAPI.SendException("An error occured while handling a message!", err, DiscordPlugin.plugin)).subscribe()
        dispatcher.on(PresenceUpdateEvent::class.java).subscribe((event: PresenceUpdateEvent) -> {
            if (!DiscordPlugin.SafeMode)
                FunModule.handleFullHouse(event)
        })
        dispatcher.on(RoleCreateEvent::class.java).^^().subscribe(GameRoleModule.handleRoleEvent _)
        dispatcher.on(RoleDeleteEvent::class.java).^^().subscribe(GameRoleModule.handleRoleEvent _)
        dispatcher.on(RoleUpdateEvent::class.java).^^().subscribe(GameRoleModule.handleRoleEvent _)
        dispatcher.on(ChatInputInteractionEvent::class.java).^^().subscribe((event: ChatInputInteractionEvent) -> {
            if (event.getCommandName equals "connect") {
                ConnectCommand().def(Command2DCSender(event), event.getOption("name").get.getValue.get.asString)
            }
        })
    }

    var debug = false

    fun debug(debug: String): Unit = if (CommonListeners.debug) { //Debug
        DPUtils.getLogger.info(debug)
    }
}