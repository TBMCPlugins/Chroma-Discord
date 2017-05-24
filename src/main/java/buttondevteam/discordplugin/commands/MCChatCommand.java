package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.lib.player.PlayerData;
import sx.blah.discord.handle.obj.IMessage;

public class MCChatCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "mcchat";
	}

	@Override
	public void run(IMessage message, String args) {
		if (!message.getChannel().isPrivate()) {
			message.reply("This command can only be issued while DMing the bot.");
			return;
		}
		PlayerData<Boolean> mcchat = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class)
				.minecraftChat();
		mcchat.set(!mcchat.get());
		message.reply("Minecraft chat " + (mcchat.get() ? "enabled." : "disabled."));
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				"mcchat enables or disables the Minecraft chat in private messages.", //
				"It can be useful if you don't want your messages to be visible, for example when talking a private channel." //
		}; // TODO: Pin channel switching to indicate the current channel
	}

}
