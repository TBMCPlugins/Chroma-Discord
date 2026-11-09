package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.DiscordPlugin
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin
import reactor.core.scala.publisher.SMono

import java.util

class DiscordSender(user: User, channel: MessageChannel, pname: String) : DiscordSenderBase(user, channel) with CommandSender {
    private val perm = PermissibleBase(this)
    private val senderName: String = Option(pname)
        .orElse(Option(user).flatMap(u -> u.asMember(DiscordPlugin.mainServer.getId).^^()
            .onErrorResume(_ -> SMono.empty).blockOption()
            .map(u -> u.getDisplayName)))
        .getOrElse("Discord user")

    fun this(user: User, channel: MessageChannel) = {
        this(user, channel, null)
    }

    override fun isPermissionSet(name: String): Boolean = perm.isPermissionSet(name)

    override fun isPermissionSet(perm: Permission): Boolean = this.perm.isPermissionSet(perm)

    override fun hasPermission(name: String): Boolean = {
        if (name.contains("essentials") && !(name == "essentials.list")) false
        else perm.hasPermission(name)
    }

    override fun hasPermission(perm: Permission): Boolean = this.perm.hasPermission(perm)

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean): PermissionAttachment = perm.addAttachment(plugin, name, value)

    override fun addAttachment(plugin: Plugin): PermissionAttachment = perm.addAttachment(plugin)

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean, ticks: Int): PermissionAttachment = perm.addAttachment(plugin, name, value, ticks)

    override fun addAttachment(plugin: Plugin, ticks: Int): PermissionAttachment = perm.addAttachment(plugin, ticks)

    override fun removeAttachment(attachment: PermissionAttachment): Unit = perm.removeAttachment(attachment)

    override fun recalculatePermissions(): Unit = perm.recalculatePermissions()

    override fun getEffectivePermissions(): util.Set[PermissionAttachmentInfo] = perm.getEffectivePermissions

    override fun isOp = false

    override fun setOp(value: Boolean): Unit = {
    }

    override fun getServer: Server = Bukkit.getServer

    override fun getName(): String = senderName

    override fun name(): Component = Component.text(senderName)

    //override fun spigot(): CommandSender.Spigot = CommandSender.Spigot
    override fun spigot(): CommandSender.Spigot = ???
}