package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.fun.FunModule
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.Timings
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import discord4j.core.`object`.entity.channel.{MessageChannel, PrivateChannel}
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.role.{RoleCreateEvent, RoleDeleteEvent, RoleUpdateEvent}
import reactor.core.publisher.Mono

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
    def register(dispatcher: EventDispatcher) = {
        dispatcher.on(classOf[MessageCreateEvent]).flatMap((event: MessageCreateEvent) => {
            def foo(event: MessageCreateEvent) = {
                timings.printElapsed("Message received")
                val `def` = Mono.empty
                if (DiscordPlugin.SafeMode) return `def`
                val author = event.getMessage.getAuthor
                if (!author.isPresent || author.get.isBot) return `def`
                if (FunModule.executeMemes(event.getMessage)) return `def`
                val commandChannel = DiscordPlugin.plugin.commandChannel.get
                event.getMessage.getChannel.map((mch: MessageChannel) => (commandChannel != null && mch.getId.asLong == commandChannel.asLong) //If mentioned, that's higher than chat
                    || mch.isInstanceOf[PrivateChannel] || event.getMessage.getContent.contains("channelcon")).flatMap(
                    (shouldRun: Boolean) => { //Only 'channelcon' is allowed in other channels
                        def foo(shouldRun: Boolean): Mono[Boolean] = { //Only continue if this doesn't handle the event
                            if (!shouldRun) return Mono.just(true) //The condition is only for the first command execution, not mcchat
                            timings.printElapsed("Run command 1")
                            CommandListener.runCommand(event.getMessage, commandChannel, mentionedonly = true) //#bot is handled here
                        }

                        foo(shouldRun)
                    }).filterWhen((ch: Any) => {
                    def foo(): Mono[Boolean] = {
                        timings.printElapsed("mcchat")
                        val mcchat = Component.getComponents.get(classOf[MinecraftChatModule])
                        if (mcchat != null && mcchat.isEnabled) { //ComponentManager.isEnabled() searches the component again
                            return mcchat.asInstanceOf[MinecraftChatModule].getListener.handleDiscord(event) //Also runs Discord commands in chat channels
                        }
                        Mono.just(true) //Wasn't handled, continue
                    }

                    foo()
                }).filterWhen((ch: Any) => {
                    def foo(ch: Any) = {
                        timings.printElapsed("Run command 2")
                        CommandListener.runCommand(event.getMessage, commandChannel, mentionedonly = false)
                    }

                    foo(ch)
                })
            }

            foo(event)
        }).onErrorContinue((err: Throwable, obj: Any) => TBMCCoreAPI.SendException("An error occured while handling a message!", err, DiscordPlugin.plugin)).subscribe
        dispatcher.on(classOf[PresenceUpdateEvent]).subscribe((event: PresenceUpdateEvent) => {
            def foo(event: PresenceUpdateEvent) = {
                if (DiscordPlugin.SafeMode) return
                FunModule.handleFullHouse(event)
            }

            foo(event)
        })
        dispatcher.on(classOf[RoleCreateEvent]).subscribe(GameRoleModule.handleRoleEvent _)
        dispatcher.on(classOf[RoleDeleteEvent]).subscribe(GameRoleModule.handleRoleEvent _)
        dispatcher.on(classOf[RoleUpdateEvent]).subscribe(GameRoleModule.handleRoleEvent _)
    }

    private var debug = false

    def debug(debug: String): Unit = if (CommonListeners.debug) { //Debug
        DPUtils.getLogger.info(debug)
    }

    def debug(): Unit = debug = !debug
}