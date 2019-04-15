package buttondevteam.discordplugin.role;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.discordplugin.commands.ICommand2DC;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import lombok.val;
import sx.blah.discord.handle.obj.IRole;

import java.util.List;
import java.util.stream.Collectors;

@CommandClass
public class RoleCommand extends ICommand2DC {

	private GameRoleModule grm;

	RoleCommand(GameRoleModule grm) {
		this.grm = grm;
	}

	@Command2.Subcommand(helpText = {
		"Add role",
		"This command adds a role to your account."
	})
	public boolean add(Command2DCSender sender, @Command2.TextArg String rolename) {
		final IRole role = checkAndGetRole(sender, rolename);
		if (role == null)
			return true;
		try {
			DPUtils.perform(() -> sender.getMessage().getAuthor().addRole(role));
			sender.sendMessage("added role.");
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while adding role!", e);
			sender.sendMessage("an error occured while adding the role.");
		}
		return true;
	}

	@Command2.Subcommand(helpText = {
		"Remove role",
		"This command removes a role from your account."
	})
	public boolean remove(Command2DCSender sender, @Command2.TextArg String rolename) {
		final IRole role = checkAndGetRole(sender, rolename);
		if (role == null)
			return true;
		try {
			DPUtils.perform(() -> sender.getMessage().getAuthor().removeRole(role));
			sender.sendMessage("removed role.");
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while removing role!", e);
			sender.sendMessage("an error occured while removing the role.");
		}
		return true;
	}

	@Command2.Subcommand
	public void list(Command2DCSender sender) {
		sender.sendMessage("list of roles:\n" + grm.GameRoles.stream().sorted().collect(Collectors.joining("\n")));
    }

	private IRole checkAndGetRole(Command2DCSender sender, String rolename) {
		String rname = rolename;
		if (!grm.GameRoles.contains(rolename)) { //If not found as-is, correct case
			val orn = grm.GameRoles.stream().filter(r -> r.equalsIgnoreCase(rolename)).findAny();
			if (!orn.isPresent()) {
				sender.sendMessage("that role cannot be found.");
				list(sender);
				return null;
			}
			rname = orn.get();
        }
		final List<IRole> roles = DiscordPlugin.mainServer.getRolesByName(rname);
        if (roles.size() == 0) {
	        sender.sendMessage("the specified role cannot be found on Discord! Removing from the list.");
	        grm.GameRoles.remove(rolename);
            return null;
        }
        if (roles.size() > 1) {
	        sender.sendMessage("there are multiple roles with this name. Why are there multiple roles with this name?");
            return null;
        }
        return roles.get(0);
    }

}
