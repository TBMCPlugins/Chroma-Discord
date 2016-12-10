package buttondevteam.discordplugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.IDiscordSender;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public abstract class DiscordSenderBase implements IDiscordSender {
	protected IUser user;
	protected IChannel channel;

	protected DiscordSenderBase(IUser user, IChannel channel) {
		this.user = user;
		this.channel = channel;
	}

	private static volatile List<String> broadcasts = new ArrayList<>();

	@Override
	public void sendMessage(String message) {
		try {
			final boolean broadcast = new Exception().getStackTrace()[2].getMethodName().contains("broadcast");
			if (broadcast) {
				if (broadcasts.contains(message))
					return;
				if (broadcasts.size() > 10)
					broadcasts.clear();
				broadcasts.add(message);
			}
			final String sendmsg = DiscordPlugin.sanitizeString(message);
			Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> DiscordPlugin
					.sendMessageToChannel(channel, (!broadcast ? user.mention() + " " : "") + sendmsg));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e);
		}
	}

	@Override
	public void sendMessage(String[] messages) {
		sendMessage(Arrays.stream(messages).collect(Collectors.joining("\n")));
	}
}
