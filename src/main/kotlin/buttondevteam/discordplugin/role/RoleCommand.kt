package buttondevteam.discordplugin.role

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.commands.Command2DCSender
import buttondevteam.discordplugin.commands.ICommand2DC
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass
import discord4j.core.`object`.entity.Role
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow

@CommandClass
class RoleCommand internal constructor(var grm: GameRoleModule) : ICommand2DC() {
    @Command2.Subcommand(
        helpText = [
            "Add role",
            "This command adds a role to your account."
        ]
    )
    suspend fun add(
        sender: Command2DCSender,
        @Command2.TextArg rolename: String
    ): Boolean { // TODO: Suspend command handler support
        val role = checkAndGetRole(sender, rolename) ?: return true
        try {
            sender.authorAsMember?.addRole(role.id)?.subscribe { sender.sendMessage("Added role.") }
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("Error while adding role!", e, grm)
            sender.sendMessage("an error occured while adding the role.")
        }
        return true
    }

    @Command2.Subcommand(
        helpText = [
            "Remove role",
            "This command removes a role from your account."
        ]
    )
    suspend fun remove(sender: Command2DCSender, @Command2.TextArg rolename: String): Boolean {
        val role = checkAndGetRole(sender, rolename) ?: return true
        try {
            sender.authorAsMember?.removeRole(role.id)?.subscribe { sender.sendMessage("Removed role.") }
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("Error while removing role!", e, grm)
            sender.sendMessage("an error occured while removing the role.")
        }
        return true
    }

    @Command2.Subcommand
    fun list(sender: Command2DCSender) {
        val sb = StringBuilder()
        var b = false
        for (role in grm.GameRoles.sorted()) {
            sb.append(role)
            if (!b) for (_ in 0 until 1.coerceAtLeast(20 - role.length)) {
                sb.append(" ")
            }
            else sb.append("\n")
            b = !b
        }
        if (sb.isNotEmpty() && sb[sb.length - 1] != '\n') sb.append('\n')
        sender.sendMessage("list of roles:\n```\n" + sb + "```")
    }

    private suspend fun checkAndGetRole(sender: Command2DCSender, rolename: String): Role? {
        var rname = rolename
        if (!grm.GameRoles.contains(rolename)) { //If not found as-is, correct case
            val orn = grm.GameRoles.find { r -> r.contentEquals(rolename, true) }
            if (orn == null) {
                sender.sendMessage("that role cannot be found.")
                list(sender)
                return null
            }
            rname = orn
        }
        val frname = rname
        val roles = DiscordPlugin.plugin.mainServer.roles.filter { r -> r.name == frname }.asFlow().toList()
        if (roles.isEmpty()) {
            sender.sendMessage("the specified role cannot be found on Discord! Removing from the list.")
            grm.GameRoles.remove(rolename)
            return null
        }
        if (roles.size > 1) {
            sender.sendMessage("there are multiple roles with this name. Why are there multiple roles with this name?")
            return null
        }
        return roles[0]
    }
}