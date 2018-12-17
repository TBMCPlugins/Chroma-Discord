package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.mcchat.MCChatPrivate;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IMessage;

public class MCChatCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "mcchat";
	}

	@Override
	public boolean run(IMessage message, String args) {
		if (!message.getChannel().isPrivate()) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"This command can only be issued in a direct message with the bot.");
			return true;
		}
		try (final DiscordPlayer user = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class)) {
			boolean mcchat = !user.isMinecraftChatEnabled();
			MCChatPrivate.privateMCChat(message.getChannel(), mcchat, message.getAuthor(), user);
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Minecraft chat " + (mcchat //
							? "enabled. Use '" + DiscordPlugin.getPrefix() + "mcchat' again to turn it off." //
							: "disabled."));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while setting mcchat for user" + message.getAuthor().getName(), e);
		}
		return true;
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				DiscordPlugin.getPrefix() + "mcchat enables or disables the Minecraft chat in private messages.", //
				"It can be useful if you don't want your messages to be visible, for example when talking in a private channel.", //
				"You can also run all of the ingame commands you have access to using this command, if you have your accounts connected." //
		}; // TODO: Pin channel switching to indicate the current channel
	}

}
