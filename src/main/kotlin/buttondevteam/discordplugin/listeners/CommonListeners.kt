package buttondevteam.discordplugin.listeners

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DPUtils.addHandler
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.commands.Command2DCSender
import buttondevteam.discordplugin.commands.ConnectCommand
import buttondevteam.discordplugin.`fun`.FunModule
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.Timings
import buttondevteam.lib.TBMCCoreAPI
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
                ComponentManager.getIfEnabled(FunModule::class.java)?.executeMemes(event.message)
                    ?.let { if (it) return@addHandler }
                val component = ComponentManager.getIfEnabled(MinecraftChatModule::class.java) ?: return@addHandler
                component.listener?.handleDiscord(event)?.let { if (it) return@addHandler }
            } catch (e: Exception) {
                if (e is CancellationException) {
                    currentCoroutineContext().ensureActive()
                }
                TBMCCoreAPI.SendException("An error occured while handling a message!", e, DiscordPlugin.plugin)
            }
        }
        dispatcher.on(PresenceUpdateEvent::class.java).addHandler { event ->
            if (!DiscordPlugin.SafeMode) ComponentManager.getIfEnabled(FunModule::class.java)?.handleFullHouse(event)
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