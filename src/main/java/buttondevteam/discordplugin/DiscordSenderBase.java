package buttondevteam.discordplugin;

import buttondevteam.lib.TBMCCoreAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.MessageChannel;

public abstract class DiscordSenderBase implements CommandSender {
	/**
	 * May be null.
	 */
	protected IUser user;
	protected MessageChannel channel;

	protected DiscordSenderBase(IUser user, MessageChannel channel) {
		this.user = user;
		this.channel = channel;
	}

	private volatile String msgtosend = "";
	private volatile BukkitTask sendtask;

	/**
	 * Returns the user. May be null.
	 * 
	 * @return The user or null.
	 */
	public IUser getUser() {
		return user;
	}

	public MessageChannel getChannel() {
		return channel;
	}

	private DiscordPlayer chromaUser;

	/**
	 * Loads the user data on first query.
	 *
	 * @return A Chroma user of Discord or a Discord user of Chroma
	 */
	public DiscordPlayer getChromaUser() {
		if (chromaUser == null) chromaUser = DiscordPlayer.getUser(user.getStringID(), DiscordPlayer.class);
		return chromaUser;
	}

	@Override
	public void sendMessage(String message) {
		try {
			final boolean broadcast = new Exception().getStackTrace()[2].getMethodName().contains("broadcast");
			//if (broadcast && DiscordPlugin.hooked) - TODO: What should happen if unhooked
			if (broadcast)
				return;
			final String sendmsg = DPUtils.sanitizeString(message);
			msgtosend += "\n" + sendmsg;
			if (sendtask == null)
				sendtask = Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
					DiscordPlugin.sendMessageToChannel(channel,
							(!broadcast && user != null ? user.mention() + "\n" : "") + msgtosend.trim());
					sendtask = null;
					msgtosend = "";
				}, 4); // Waits a 0.2 second to gather all/most of the different messages
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e);
		}
	}

	@Override
	public void sendMessage(String[] messages) {
		sendMessage(String.join("\n", messages));
	}
}
