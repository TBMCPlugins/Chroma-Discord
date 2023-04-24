package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.DiscordPlugin
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.command.CommandSender
import org.bukkit.permissions.{PermissibleBase, Permission, PermissionAttachment, PermissionAttachmentInfo}
import org.bukkit.plugin.Plugin
import org.bukkit.{Bukkit, Server}
import reactor.core.publisher.Mono
import scala.jdk.OptionConverters._

import java.util

class DiscordSender(user: User, channel: MessageChannel, pname: String) extends DiscordSenderBase(user, channel) with CommandSender {
    private val perm = new PermissibleBase(this)
    private val name: String = Option(pname)
        .orElse(Option(user).flatMap(u => u.asMember(DiscordPlugin.mainServer.getId)
            .onErrorResume(_ => Mono.empty).blockOptional().toScala
            .map(u => u.getDisplayName)))
        .getOrElse("Discord user")

    def this(user: User, channel: MessageChannel) = {
        this(user, channel, null)
    }

    override def isPermissionSet(name: String): Boolean = perm.isPermissionSet(name)

    override def isPermissionSet(perm: Permission): Boolean = this.perm.isPermissionSet(perm)

    override def hasPermission(name: String): Boolean = {
        if (name.contains("essentials") && !(name == "essentials.list")) false
        else perm.hasPermission(name)
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

    //override def spigot(): CommandSender.Spigot = new CommandSender.Spigot
    override def spigot(): CommandSender.Spigot = ???
}