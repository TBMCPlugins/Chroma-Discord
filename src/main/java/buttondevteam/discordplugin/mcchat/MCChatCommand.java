package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.discordplugin.commands.ICommand2DC;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import lombok.val;

@CommandClass(helpText = {
	"MC Chat",
	"This command enables or disables the Minecraft chat in private messages.", //
	"It can be useful if you don't want your messages to be visible, for example when talking in a private channel.", //
	"You can also run all of the ingame commands you have access to using this command, if you have your accounts connected." //
})
public class MCChatCommand extends ICommand2DC {

	@Command2.Subcommand
	public boolean def(Command2DCSender sender) {
		val message = sender.getMessage();
		if (!message.getChannel().isPrivate()) {
			message.reply("this command can only be issued in a direct message with the bot.");
			return true;
		}
		try (final DiscordPlayer user = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class)) {
			boolean mcchat = !user.isMinecraftChatEnabled();
			MCChatPrivate.privateMCChat(message.getChannel(), mcchat, message.getAuthor(), user);
			message.reply("Minecraft chat " + (mcchat //
				? "enabled. Use '" + DiscordPlugin.getPrefix() + "mcchat' again to turn it off." //
				: "disabled."));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while setting mcchat for user" + message.getAuthor().getName(), e);
		}
		return true;
	} // TODO: Pin channel switching to indicate the current channel

}
