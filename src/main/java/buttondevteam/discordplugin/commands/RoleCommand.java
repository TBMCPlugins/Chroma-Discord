package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.handle.obj.IMessage;

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
		} else if (argsa[0].equalsIgnoreCase("remove")) {
			if (argsa.length < 2) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"This command removes a game role from your account.\nUsage: remove <rolename>");
				return;
			}
		} else if (argsa[0].equalsIgnoreCase("list")) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "List of game roles:");
		}
	}

	@Override
	public String[] getHelpText() {
		// TODO Auto-generated method stub
		return null;
	}

}
