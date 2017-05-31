package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.lib.TBMCCoreAPI;
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
		try (final DiscordPlayer user = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class)) {
			PlayerData<Boolean> mcchat = user.minecraftChat();
			mcchat.set(!mcchat.getOrDefault(false));
			message.reply("Minecraft chat " + (mcchat.get() ? "enabled." : "disabled."));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while setting mcchat for user" + message.getAuthor().getName(), e);
		}
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				"mcchat enables or disables the Minecraft chat in private messages.", //
				"It can be useful if you don't want your messages to be visible, for example when talking a private channel." //
		}; // TODO: Pin channel switching to indicate the current channel
	}

}
