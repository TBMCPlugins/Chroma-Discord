package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.discordplugin.commands.ICommand2DC;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import discord4j.core.object.entity.PrivateChannel;
import lombok.RequiredArgsConstructor;
import lombok.val;

@CommandClass(helpText = {
	"MC Chat",
	"This command enables or disables the Minecraft chat in private messages.", //
	"It can be useful if you don't want your messages to be visible, for example when talking in a private channel.", //
	"You can also run all of the ingame commands you have access to using this command, if you have your accounts connected." //
})
@RequiredArgsConstructor
public class MCChatCommand extends ICommand2DC {

	private final MinecraftChatModule module;

	@Command2.Subcommand
	public boolean def(Command2DCSender sender) {
		if (!module.allowPrivateChat().get()) {
			sender.sendMessage("using the private chat is not allowed on this Minecraft server.");
			return true;
		}
		val message = sender.getMessage();
		val channel = message.getChannel().block();
		@SuppressWarnings("OptionalGetWithoutIsPresent") val author = message.getAuthor().get();
		if (!(channel instanceof PrivateChannel)) {
			DPUtils.reply(message, channel, "this command can only be issued in a direct message with the bot.").subscribe();
			return true;
		}
		try (final DiscordPlayer user = DiscordPlayer.getUser(author.getId().asString(), DiscordPlayer.class)) {
			boolean mcchat = !user.isMinecraftChatEnabled();
			MCChatPrivate.privateMCChat(channel, mcchat, author, user);
			DPUtils.reply(message, channel, "Minecraft chat " + (mcchat //
				? "enabled. Use '" + DiscordPlugin.getPrefix() + "mcchat' again to turn it off." //
				: "disabled.")).subscribe();
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while setting mcchat for user " + author.getUsername() + "#" + author.getDiscriminator(), e);
		}
		return true;
	} // TODO: Pin channel switching to indicate the current channel

}
