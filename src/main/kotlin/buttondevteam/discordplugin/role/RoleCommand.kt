package buttondevteam.discordplugin.role

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.commands.{Command2DCSender, ICommand2DC}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.chat.{Command2, CommandClass}
import discord4j.core.`object`.entity.Role
import reactor.core.publisher.Mono

@CommandClass class RoleCommand private[role](var grm: GameRoleModule) extends ICommand2DC {
    @Command2.Subcommand(helpText = Array(
        "Add role",
        "This command adds a role to your account."
    )) def add(sender: Command2DCSender, @Command2.TextArg rolename: String): Boolean = {
        val role = checkAndGetRole(sender, rolename)
        if (role == null) return true
        try sender.authorAsMember.foreach(m => m.addRole(role.getId).subscribe(_ => sender.sendMessage("Added role.")))
        catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("Error while adding role!", e, grm)
                sender.sendMessage("an error occured while adding the role.")
        }
        true
    }

    @Command2.Subcommand(helpText = Array(
        "Remove role",
        "This command removes a role from your account."
    )) def remove(sender: Command2DCSender, @Command2.TextArg rolename: String): Boolean = {
        val role = checkAndGetRole(sender, rolename)
        if (role == null) return true
        try sender.authorAsMember.foreach(m => m.removeRole(role.getId).subscribe(_ => sender.sendMessage("Removed role.")))
        catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("Error while removing role!", e, grm)
                sender.sendMessage("an error occured while removing the role.")
        }
        true
    }

    @Command2.Subcommand def list(sender: Command2DCSender): Unit = {
        val sb = new StringBuilder
        var b = false
        for (role <- grm.GameRoles.sorted.iterator.asInstanceOf[Iterable[String]]) {
            sb.append(role)
            if (!b) for (_ <- 0 until Math.max(1, 20 - role.length)) {
                sb.append(" ")
            }
            else sb.append("\n")
            b = !b
        }
        if (sb.nonEmpty && sb.charAt(sb.length - 1) != '\n') sb.append('\n')
        sender.sendMessage("list of roles:\n```\n" + sb + "```")
    }

    private def checkAndGetRole(sender: Command2DCSender, rolename: String): Role = {
        var rname = rolename
        if (!grm.GameRoles.contains(rolename)) { //If not found as-is, correct case
            val orn = grm.GameRoles.find(r => r.equalsIgnoreCase(rolename))
            if (orn.isEmpty) {
                sender.sendMessage("that role cannot be found.")
                list(sender)
                return null
            }
            rname = orn.get
        }
        val frname = rname
        val roles = DiscordPlugin.mainServer.getRoles.filter(r => r.getName.equals(frname)).collectList.block
        if (roles == null) {
            sender.sendMessage("an error occured.")
            return null
        }
        if (roles.size == 0) {
            sender.sendMessage("the specified role cannot be found on Discord! Removing from the list.")
            grm.GameRoles.subtractOne(rolename)
            return null
        }
        if (roles.size > 1) {
            sender.sendMessage("there are multiple roles with this name. Why are there multiple roles with this name?")
            return null
        }
        roles.get(0)
    }
}