package buttondevteam.discordplugin.role

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DPUtils.{FluxExtensions, MonoExtensions}
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.architecture.{Component, ComponentMetadata}
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.event.domain.role.{RoleCreateEvent, RoleDeleteEvent, RoleEvent, RoleUpdateEvent}
import discord4j.rest.util.Color
import org.bukkit.Bukkit
import reactor.core.publisher.Mono
import reactor.core.scala.publisher.SMono

import java.util.Collections
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.SeqHasAsJava

/**
 * Automatically collects roles with a certain color.
 * Users can add these roles to themselves using the /role Discord command.
 */
@ComponentMetadata(enabledByDefault = false) object GameRoleModule {
    def handleRoleEvent(roleEvent: RoleEvent): Unit = {
        val grm = ComponentManager.getIfEnabled(classOf[GameRoleModule])
        if (grm == null) return ()
        val GameRoles = grm.GameRoles
        val logChannel = grm.logChannel.get
        val notMainServer = (_: Role).getGuildId.asLong != DiscordPlugin.mainServer.getId.asLong

        def removeFromList = (name: String) => {
            val c = GameRoles.size
            GameRoles.subtractOne(name)
            c > GameRoles.size
        }

        roleEvent match {
            case roleCreateEvent: RoleCreateEvent => Bukkit.getScheduler.runTaskLaterAsynchronously(DiscordPlugin.plugin, () => {
                val role = roleCreateEvent.getRole
                if (!notMainServer(role)) {
                    grm.isGameRole(role).flatMap(b => {
                        if (!b) SMono.empty //Deleted or not a game role
                        else {
                            GameRoles.addOne(role.getName)
                            if (logChannel != null)
                                logChannel.flatMap(_.createMessage("Added " + role.getName + " as game role." +
                                    " If you don't want this, change the role's color from the game role color.").^^())
                            else
                                SMono.empty
                        }
                    }).subscribe()
                    ()
                }
            }, 100)
            case roleDeleteEvent: RoleDeleteEvent =>
                val role = roleDeleteEvent.getRole.orElse(null)
                if (role == null) return ()
                if (notMainServer(role)) return ()
                if (removeFromList(role.getName) && logChannel != null)
                    logChannel.flatMap(_.createMessage("Removed " + role.getName + " as a game role.").^^()).subscribe()
            case roleUpdateEvent: RoleUpdateEvent =>
                if (!roleUpdateEvent.getOld.isPresent) {
                    grm.logWarn("Old role not stored, cannot update game role!")
                    return ()
                }
                val or = roleUpdateEvent.getOld.get
                if (notMainServer(or)) return ()
                val cr = roleUpdateEvent.getCurrent
                grm.isGameRole(cr).flatMap(isGameRole => {
                    if (!isGameRole)
                        if (removeFromList(or.getName) && logChannel != null)
                            logChannel.flatMap(_.createMessage("Removed " + or.getName + " as a game role because its color changed.").^^())
                        else
                            SMono.empty
                    else if (GameRoles.contains(or.getName) && or.getName == cr.getName)
                        SMono.empty
                    else {
                        val removed = removeFromList(or.getName) //Regardless of whether it was a game role
                        GameRoles.addOne(cr.getName) //Add it because it has no color
                        if (logChannel != null)
                            if (removed)
                                logChannel.flatMap((ch: MessageChannel) => ch.createMessage("Changed game role from " + or.getName + " to " + cr.getName + ".").^^())
                            else
                                logChannel.flatMap((ch: MessageChannel) => ch.createMessage("Added " + cr.getName + " as game role because it has the color of one.").^^())
                        else
                            SMono.empty
                    }
                }).subscribe()
            case _ =>
        }
    }
}

@ComponentMetadata(enabledByDefault = false) class GameRoleModule extends Component[DiscordPlugin] {
    var GameRoles: mutable.Buffer[String] = null
    final private val command = new RoleCommand(this)

    override protected def enable(): Unit = {
        getPlugin.manager.registerCommand(command)
        GameRoles = DiscordPlugin.mainServer.getRoles.^^().filterWhen(this.isGameRole).map(_.getName).collectSeq().block().toBuffer
    }

    override protected def disable(): Unit = getPlugin.manager.unregisterCommand(command)

    /**
     * The channel where the bot logs when it detects a role change that results in a new game role or one being removed.
     */
    final private def logChannel = DPUtils.channelData(getConfig, "logChannel")
    /**
     * The role color that is used by game roles.
     * Defaults to the second to last in the upper row - #95a5a6.
     */
    final private def roleColor = getConfig.getData("roleColor", Color.of(149, 165, 166),
        (rgb: Any) => Color.of(Integer.parseInt(rgb.asInstanceOf[String].substring(1), 16)),
        (color: Color) => String.format("#%08x", color.getRGB), true)

    private def isGameRole(r: Role): SMono[Boolean] = {
        if (r.getGuildId.asLong != DiscordPlugin.mainServer.getId.asLong) return SMono.just(false) //Only allow on the main server
        val rc = roleColor.get
        if (r.getColor equals rc)
            DiscordPlugin.dc.getSelf.^^().flatMap(u => u.asMember(DiscordPlugin.mainServer.getId).^^())
                .flatMap(_.hasHigherRoles(Collections.singleton(r.getId)).^^()).cast[Boolean].defaultIfEmpty(false) //Below one of our roles
        else SMono.just(false)
    }
}