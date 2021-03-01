package buttondevteam.discordplugin

import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.{Bukkit, Server}
import org.bukkit.command.CommandSender
import org.bukkit.permissions.{PermissibleBase, Permission, PermissionAttachment, PermissionAttachmentInfo}
import org.bukkit.plugin.Plugin
import reactor.core.publisher.Mono

import java.util

class DiscordSender(user: User, channel: MessageChannel) extends DiscordSenderBase(user, channel) with CommandSender {
    private val perm = new PermissibleBase(this)
    private var name: String = null

    def this(user: User, channel: MessageChannel) {
        this(user, channel)
        val `def` = "Discord user"
        name = if (user == null) `def`
        else user.asMember(DiscordPlugin.mainServer.getId).onErrorResume((_: Throwable) => Mono.empty).blockOptional.map(_.getDisplayName).orElse(`def`)
    }

    def this(user: User, channel: MessageChannel, name: String) {
        this(user, channel)
        this.name = name
    }

    override def isPermissionSet(name: String): Boolean = perm.isPermissionSet(name)

    override def isPermissionSet(perm: Permission): Boolean = this.perm.isPermissionSet(perm)

    override def hasPermission(name: String): Boolean = {
        if (name.contains("essentials") && !(name == "essentials.list")) return false
        perm.hasPermission(name)
    }

    override def hasPermission(perm: Permission): Boolean = this.perm.hasPermission(perm)

    override def addAttachment(plugin: Plugin, name: String, value: Boolean): PermissionAttachment = perm.addAttachment(plugin, name, value)

    override def addAttachment(plugin: Plugin): PermissionAttachment = perm.addAttachment(plugin)

    override def addAttachment(plugin: Plugin, name: String, value: Boolean, ticks: Int): PermissionAttachment = perm.addAttachment(plugin, name, value, ticks)

    override def addAttachment(plugin: Plugin, ticks: Int): PermissionAttachment = perm.addAttachment(plugin, ticks)

    override def removeAttachment(attachment: PermissionAttachment): Unit = perm.removeAttachment(attachment)

    override def recalculatePermissions(): Unit = perm.recalculatePermissions()

    override def getEffectivePermissions: util.Set[PermissionAttachmentInfo] = perm.getEffectivePermissions

    override def isOp = false

    override def setOp(value: Boolean): Unit = {
    }

    override def getServer: Server = Bukkit.getServer

    override def getName: String = name

    override def spigot = new CommandSender.Spigot
}