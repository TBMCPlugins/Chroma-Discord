package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.DiscordPlugin
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Server
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissibleBase
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.permissions.PermissionAttachmentInfo
import org.bukkit.plugin.Plugin
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class DiscordSender(user: User?, channel: MessageChannel, pname: String?) : DiscordSenderBase(user, channel), CommandSender {
    private val perm = PermissibleBase(this)
    private val senderName: String = pname ?: user?.asMember(DiscordPlugin.plugin!!.mainServer!!.id)?.blockOptional()?.getOrNull()?.displayName ?: "Discord user"

    constructor(user: User, channel: MessageChannel) : this(user, channel, null) {
    }

    override fun isPermissionSet(name: String): Boolean = perm.isPermissionSet(name)

    override fun isPermissionSet(perm: Permission): Boolean = this.perm.isPermissionSet(perm)

    override fun hasPermission(name: String): Boolean {
        return if (name.contains("essentials") && name != "essentials.list") false
        else perm.hasPermission(name)
    }

    override fun hasPermission(perm: Permission): Boolean = this.perm.hasPermission(perm)

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean): PermissionAttachment = perm.addAttachment(plugin, name, value)

    override fun addAttachment(plugin: Plugin): PermissionAttachment = perm.addAttachment(plugin)

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean, ticks: Int): PermissionAttachment? = perm.addAttachment(plugin, name, value, ticks)

    override fun addAttachment(plugin: Plugin, ticks: Int): PermissionAttachment? = perm.addAttachment(plugin, ticks)

    override fun removeAttachment(attachment: PermissionAttachment): Unit = perm.removeAttachment(attachment)

    override fun recalculatePermissions(): Unit = perm.recalculatePermissions()

    override fun getEffectivePermissions(): Set<PermissionAttachmentInfo> = perm.effectivePermissions

    override fun isOp() = false

    override fun setOp(value: Boolean) {
    }

    override fun getServer(): Server = Bukkit.getServer()

    override fun getName(): String = senderName

    override fun name(): Component = Component.text(senderName)

    override fun spigot(): CommandSender.Spigot = CommandSender.Spigot()
}