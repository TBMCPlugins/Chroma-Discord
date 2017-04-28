package buttondevteam.discordplugin.commands;

import java.util.List;
import java.util.stream.Collectors;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

public class RoleCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "role";
	}

	@Override
	public void run(IMessage message, String args) {
		final String usagemsg = "Subcommands: add, remove, list";
		if (args.length() == 0) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), usagemsg);
			return;
		}
		String[] argsa = args.split(" ");
		if (argsa[0].equalsIgnoreCase("add")) {
			final IRole role = checkAndGetRole(message, argsa, "This command adds a game role to your account.");
			if (role == null)
				return;
			try {
				DiscordPlugin.perform(() -> message.getAuthor().addRole(role));
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "Added game role.");
			} catch (Exception e) {
				TBMCCoreAPI.SendException("Error while adding role!", e);
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "An error occured while adding the role.");
			}
		} else if (argsa[0].equalsIgnoreCase("remove")) {
			final IRole role = checkAndGetRole(message, argsa, "This command removes a game role from your account.");
			if (role == null)
				return;
			try {
				DiscordPlugin.perform(() -> message.getAuthor().removeRole(role));
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "Removed game role.");
			} catch (Exception e) {
				TBMCCoreAPI.SendException("Error while removing role!", e);
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "An error occured while removing the role.");
			}
		} else if (argsa[0].equalsIgnoreCase("list")) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"List of game roles:\n" + DiscordPlugin.GameRoles.stream().collect(Collectors.joining("\n")));
		} else if (argsa[0].equalsIgnoreCase("addrole")) {
			if (!message.getAuthor().getRolesForGuild(DiscordPlugin.mainServer).stream()
					.anyMatch(r -> r.getID().equals("126030201472811008"))) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"You need to be a moderator to use this command.");
				return;
			}
			if (argsa.length < 2) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"Add a role to the game role list.\nUsage: " + argsa[0] + " <rolename>");
				return;
			}
			String rolename = argsa[1];
			for (int i = 2; i < argsa.length; i++)
				rolename += " " + argsa[i];
			final List<IRole> roles = (TBMCCoreAPI.IsTestServer() ? DiscordPlugin.devServer : DiscordPlugin.mainServer)
					.getRolesByName(rolename);
			if (roles.size() == 0) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "That role cannot be found on Discord.");
				return;
			}
			if (roles.size() > 1) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"There are more roles with this name. Why are there more roles with this name?");
				return;
			}
			DiscordPlugin.GameRoles.add(roles.get(0).getName());
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "Game role added.");
		}
	}

	private IRole checkAndGetRole(IMessage message, String[] argsa, String usage) {
		if (argsa.length < 2) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), usage + "\nUsage: " + argsa[0] + " <rolename>");
			return null;
		}
		String rolename = argsa[1];
		for (int i = 2; i < argsa.length; i++)
			rolename += " " + argsa[i];
		if (!DiscordPlugin.GameRoles.contains(rolename)) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"That game role cannot be found.\nList of game roles:\n"
							+ DiscordPlugin.GameRoles.stream().collect(Collectors.joining("\n")));
			return null;
		}
		final List<IRole> roles = (TBMCCoreAPI.IsTestServer() ? DiscordPlugin.devServer : DiscordPlugin.mainServer)
				.getRolesByName(rolename);
		if (roles.size() == 0) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"The specified role cannot be found on Discord! Removing from the list.");
			DiscordPlugin.GameRoles.remove(rolename);
			return null;
		}
		if (roles.size() > 1) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"There are more roles with this name. Why are there more roles with this name?");
			return null;
		}
		return roles.get(0);
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				"Add or remove game roles from yourself.", //
				"Usage: role add|remove <name> or role list", //
				"Mods can use role addrole <name> to add a role as a game role" };
	}

}
