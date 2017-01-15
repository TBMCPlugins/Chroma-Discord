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
			if (argsa.length < 2) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"This command adds a game role to your account.\nUsage: add <rolename>");
				return;
			}
			if (!DiscordPlugin.GameRoles.contains(argsa[1].toLowerCase())) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"That game role cannot be found.\nList of game roles:\n"
								+ DiscordPlugin.GameRoles.stream().collect(Collectors.joining("\n")));
				return;
			}
			final List<IRole> roles = (TBMCCoreAPI.IsTestServer() ? DiscordPlugin.devServer : DiscordPlugin.mainServer)
					.getRolesByName(argsa[1]);
			if (roles.size() == 0) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"The specified role cannot be found on Discord! Removing from the list.");
				DiscordPlugin.GameRoles.remove(argsa[1].toLowerCase());
				return;
			}
			if (roles.size() > 1) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"There are more roles with this name. Why are there more roles with this name?");
				return;
			}
			try {
				DiscordPlugin.perform(() -> message.getAuthor().addRole(roles.get(0)));
			} catch (Exception e) {
				TBMCCoreAPI.SendException("Error while adding role!", e);
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "An error occured while adding the role.");
			}
		} else if (argsa[0].equalsIgnoreCase("remove"))

		{
			if (argsa.length < 2) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"This command removes a game role from your account.\nUsage: remove <rolename>");
				return;
			}
		} else if (argsa[0].equalsIgnoreCase("list")) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"List of game roles:\n" + DiscordPlugin.GameRoles.stream().collect(Collectors.joining("\n")));
		}
	}

	@Override
	public String[] getHelpText() {
		// TODO Auto-generated method stub
		return null;
	}

}
