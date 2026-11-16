package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DPUtils.addHandler
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.FunModule
import buttondevteam.discordplugin.commands.Command2DCSender
import buttondevteam.discordplugin.commands.ConnectCommand
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.Timings
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.event.domain.interaction.ChatInputInteractionEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.event.domain.role.RoleCreateEvent
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.event.domain.role.RoleUpdateEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

object CommonListeners {
    val timings = Timings()

    fun register(dispatcher: EventDispatcher) {
        dispatcher.on(MessageCreateEvent::class.java).addHandler { event ->
            try {
                if (DiscordPlugin.SafeMode || event.message.author.filter { !it.isBot }.isEmpty) return@addHandler
                if (!FunModule.executeMemes(event.message)) return@addHandler
                val component = Component.components[MinecraftChatModule::class.java] as MinecraftChatModule?
                if (component == null || !component.isEnabled) return@addHandler
                if (!(component.listener?.handleDiscord(event) ?: true)) return@addHandler
            } catch (e: Exception) {
                if (e is CancellationException) {
                    currentCoroutineContext().ensureActive()
                }
                TBMCCoreAPI.SendException("An error occured while handling a message!", e, DiscordPlugin.plugin!!)
            }
        }
        dispatcher.on(PresenceUpdateEvent::class.java).addHandler { event ->
            if (!DiscordPlugin.SafeMode) FunModule.handleFullHouse(event)
        }
        dispatcher.on(RoleCreateEvent::class.java).addHandler(GameRoleModule::handleRoleEvent)
        dispatcher.on(RoleDeleteEvent::class.java).addHandler(GameRoleModule::handleRoleEvent)
        dispatcher.on(RoleUpdateEvent::class.java).addHandler(GameRoleModule::handleRoleEvent)
        dispatcher.on(ChatInputInteractionEvent::class.java).addHandler { event ->
            if (event.commandName == "connect") {
                ConnectCommand().def(Command2DCSender(event), event.getOption("name").get().value.get().asString())
            }
        }
    }

    var debug = false

    fun debug(debug: String): Unit {
        if (CommonListeners.debug) { //Debug
            DPUtils.logger.info(debug)
        }
    }
}