package buttondevteam.discordplugin.role

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DPUtils.channelData
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.architecture.Component
import buttondevteam.lib.architecture.ComponentMetadata
import com.github.shynixn.mccoroutine.bukkit.launch
import discord4j.core.event.domain.role.RoleCreateEvent
import discord4j.core.event.domain.role.RoleDeleteEvent
import discord4j.core.event.domain.role.RoleEvent
import discord4j.core.event.domain.role.RoleUpdateEvent
import discord4j.core.`object`.entity.Role
import discord4j.rest.util.Color
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlin.jvm.optionals.getOrNull

/**
 * Automatically collects roles with a certain color.
 * Users can add these roles to themselves using the /role Discord command.
 */
@ComponentMetadata(enabledByDefault = false)
class GameRoleModule : Component<DiscordPlugin>() {
    companion object {
        suspend fun handleRoleEvent(roleEvent: RoleEvent) {
            val grm = ComponentManager.getIfEnabled(GameRoleModule::class.java) ?: return
            val GameRoles = grm.GameRoles
            val logChannel = grm.logChannel.getEntity()
            val notMainServer =
                { role: Role -> role.guildId.asLong() != DiscordPlugin.plugin!!.mainServer!!.id.asLong() }

            when (roleEvent) {
                is RoleCreateEvent -> {
                    val role = roleEvent.role
                    if (!notMainServer(role)) {
                        if (grm.isGameRole(role)) {
                            GameRoles += role.name
                            logChannel
                                ?.createMessage("Added ${role.name} as game role. If you don't want this, change the role's color from the game role color.")
                                ?.awaitFirstOrNull()
                        }
                    }
                }

                is RoleDeleteEvent -> {
                    val role = roleEvent.role.getOrNull() ?: return
                    if (notMainServer(role)) return
                    if (GameRoles.remove(role.name) && logChannel != null) {
                        logChannel.createMessage("Removed ${role.name} as a game role.").awaitFirstOrNull()
                    }
                }

                is RoleUpdateEvent -> {
                    val or = roleEvent.old.getOrNull()
                    if (or == null) {
                        grm.logWarn("Old role not stored, cannot update game role!")
                        return
                    }
                    if (notMainServer(or)) return
                    val cr = roleEvent.current
                    if (!grm.isGameRole(cr)) {
                        if (GameRoles.remove(cr.name) && logChannel != null) {
                            logChannel.createMessage("Removed ${cr.name} as a game role because its color changed.")
                                .awaitFirstOrNull()
                        }
                    } else {
                        if (GameRoles.contains(or.name) && or.name == cr.name) return
                        val removed = GameRoles.remove(or.name); //Regardless of whether it was a game role
                        GameRoles.add(cr.name) //Add it because it has no color
                        if (logChannel != null) {
                            if (removed) logChannel.createMessage("Changed game role from ${or.name} to ${cr.name}.")
                                .awaitFirstOrNull()
                            else logChannel.createMessage("Added ${cr.name} as game role because it has the color of one.")
                                .awaitFirstOrNull()
                        }
                    }
                }
            }
        }
    }

    var GameRoles = mutableListOf<String>()
    private val command = RoleCommand(this)

    override fun enable() {
        plugin.manager.registerCommand(command)
        plugin.launch { enableAsync() }
    }

    private suspend fun enableAsync() {
        GameRoles.clear()
        DiscordPlugin.plugin!!.mainServer!!.roles.asFlow().filter { this.isGameRole(it) }.map { it.name }
            .toList(GameRoles)
    }

    override fun disable(): Unit = plugin.manager.unregisterCommand(command)

    /**
     * The channel where the bot logs when it detects a role change that results in a new game role or one being removed.
     */
    private val logChannel = config.channelData("logChannel")

    /**
     * The role color that is used by game roles.
     * Defaults to the second to last in the upper row - #95a5a6.
     */
    private var roleColor by config.getData(
        "roleColor", Color.of(149, 165, 166),
        { rgb -> Color.of(Integer.parseInt((rgb as String).substring(1), 16)) },
        { color -> String.format("#%08x", color.rgb) }, true
    )

    private suspend fun isGameRole(r: Role): Boolean {
        if (r.guildId.asLong() != DiscordPlugin.plugin!!.mainServer!!.id.asLong()) return false //Only allow on the main server
        val rc = roleColor
        if (r.primaryColor == rc)
            return DiscordPlugin.dc.self.awaitFirstOrNull()
                ?.asMember(DiscordPlugin.plugin!!.mainServer!!.id)?.awaitFirstOrNull()
                ?.hasHigherRoles(setOf(r.id))?.awaitFirstOrNull() ?: false
        else return false
    }
}