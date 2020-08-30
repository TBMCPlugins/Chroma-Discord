package buttondevteam.discordplugin.role;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.discordplugin.commands.ICommand2DC;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import discord4j.core.object.entity.Role;
import lombok.val;
import reactor.core.publisher.Mono;

import java.util.List;

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
		final Role role = checkAndGetRole(sender, rolename);
		if (role == null)
			return true;
		try {
			sender.getMessage().getAuthorAsMember()
				.flatMap(m -> m.addRole(role.getId()).switchIfEmpty(Mono.fromRunnable(() -> sender.sendMessage("added role."))))
				.subscribe();
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
		final Role role = checkAndGetRole(sender, rolename);
		if (role == null)
			return true;
		try {
			sender.getMessage().getAuthorAsMember()
				.flatMap(m -> m.removeRole(role.getId()).switchIfEmpty(Mono.fromRunnable(() -> sender.sendMessage("removed role."))))
				.subscribe();
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while removing role!", e);
			sender.sendMessage("an error occured while removing the role.");
		}
		return true;
	}

	@Command2.Subcommand
	public void list(Command2DCSender sender) {
		var sb = new StringBuilder();
		boolean b = false;
		for (String role : (Iterable<String>) grm.GameRoles.stream().sorted()::iterator) {
			sb.append(role);
			if (!b)
				for (int j = 0; j < Math.max(1, 20 - role.length()); j++)
					sb.append(" ");
			else
				sb.append("\n");
			b = !b;
		}
		if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n')
			sb.append('\n');
		sender.sendMessage("list of roles:\n```\n" + sb + "```");
	}

	private Role checkAndGetRole(Command2DCSender sender, String rolename) {
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
		val frname = rname;
		final List<Role> roles = DiscordPlugin.mainServer.getRoles().filter(r -> r.getName().equals(frname)).collectList().block();
		if (roles == null) {
			sender.sendMessage("an error occured.");
			return null;
		}
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
