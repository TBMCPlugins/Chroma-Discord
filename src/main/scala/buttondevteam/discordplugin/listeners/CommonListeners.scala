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
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.role.{RoleCreateEvent, RoleDeleteEvent, RoleUpdateEvent}
import reactor.core.Disposable
import reactor.core.scala.publisher.{SFlux, SMono}

object CommonListeners {
    val timings = new Timings

    /*
      MentionEvent:
      - CommandListener (starts with mention, only 'channelcon' and not in #bot)

      MessageReceivedEvent:
      - v CommandListener (starts with mention, in #bot or a connected chat)
      - Minecraft chat (is enabled in the channel and message isn't [/]mcchat)
      - CommandListener (with the correct prefix in #bot, or in private)
      */
    def register(dispatcher: EventDispatcher): Unit = {
        dispatcher.on(classOf[MessageCreateEvent]).flatMap((event: MessageCreateEvent) => {
            SMono.just(event.getMessage).filter(_ => !DiscordPlugin.SafeMode)
                .filter(message => message.getAuthor.filter(!_.isBot).isPresent)
                .filter(message => !FunModule.executeMemes(message))
                .flatMap(handleMessage(event))
        }).onErrorContinue((err, _) => TBMCCoreAPI.SendException("An error occured while handling a message!", err, DiscordPlugin.plugin)).subscribe()
        dispatcher.on(classOf[PresenceUpdateEvent]).subscribe((event: PresenceUpdateEvent) => {
            if (!DiscordPlugin.SafeMode)
                FunModule.handleFullHouse(event)
        })
        SFlux(dispatcher.on(classOf[RoleCreateEvent])).subscribe(GameRoleModule.handleRoleEvent)
        SFlux(dispatcher.on(classOf[RoleDeleteEvent])).subscribe(GameRoleModule.handleRoleEvent)
        SFlux(dispatcher.on(classOf[RoleUpdateEvent])).subscribe(GameRoleModule.handleRoleEvent)
    }

    var debug = false

    def debug(debug: String): Unit = if (CommonListeners.debug) { //Debug
        DPUtils.getLogger.info(debug)
    }

    private def handleMessage(event: MessageCreateEvent) = {
        (message: Message) => {
            val commandChannel = Option(DiscordPlugin.plugin.commandChannel.get)
            SMono(message.getChannel).filter(mch => commandChannel.isDefined && mch.getId.asLong() == commandChannel.get.asLong() //If mentioned, that's higher than chat
                || mch.isInstanceOf[PrivateChannel] || message.getContent.contains("channelcon")).flatMap(_ => { //Only 'channelcon' is allowed in other channels
                //Only continue if this doesn't handle the event
                CommandListener.runCommand(message, commandChannel.get, mentionedonly = true) //#bot is handled here
            }).`then`(SMono.just(true)) //The condition is only for the first command execution, not mcchat
                .filterWhen(_ => {
                    Option(Component.getComponents.get(classOf[MinecraftChatModule])).filter(_.isEnabled)
                        .map(_.asInstanceOf[MinecraftChatModule].getListener.handleDiscord(event)) //Also runs Discord commands in chat channels
                        .getOrElse(SMono.just(true)) //Wasn't handled, continue
                }).filterWhen(_ => CommandListener.runCommand(event.getMessage, commandChannel.get, mentionedonly = false))
        }
    }
}