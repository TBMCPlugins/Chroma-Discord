package buttondevteam.discordplugin.commands;

import sx.blah.discord.handle.obj.IMessage;

public class ConnectCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "connect";
	}

	@Override
	public void run(IMessage message, String args) { // TODO: Throws?
		try {
			message.getChannel().sendMessage("Connect command WIP.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
