package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.listeners.MCListener;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;

import java.util.ArrayList;

public class MCChatPrivate {

	/**
	 * Used for messages in PMs (mcchat).
	 */
	static ArrayList<MCChatUtils.LastMsgData> lastmsgPerUser = new ArrayList<>();

	public static boolean privateMCChat(IChannel channel, boolean start, IUser user, DiscordPlayer dp) {
		TBMCPlayer mcp = dp.getAs(TBMCPlayer.class);
		if (mcp != null) { // If the accounts aren't connected, can't make a connected sender
			val p = Bukkit.getPlayer(mcp.getUUID());
			val op = Bukkit.getOfflinePlayer(mcp.getUUID());
			if (start) {
				val sender = new DiscordConnectedPlayer(user, channel, mcp.getUUID(), op.getName());
				MCChatUtils.addSender(MCChatUtils.ConnectedSenders, user, sender);
				if (p == null)// Player is offline - If the player is online, that takes precedence
					MCListener.callEventExcludingSome(new PlayerJoinEvent(sender, ""));
			} else {
				val sender = MCChatUtils.removeSender(MCChatUtils.ConnectedSenders, channel, user);
				if (p == null)// Player is offline - If the player is online, that takes precedence
					MCListener.callEventExcludingSome(new PlayerQuitEvent(sender, ""));
			}
		}
		if (!start)
			MCChatUtils.lastmsgfromd.remove(channel.getLongID());
		return start //
				? lastmsgPerUser.add(new MCChatUtils.LastMsgData(channel, user)) // Doesn't support group DMs
				: lastmsgPerUser.removeIf(lmd -> lmd.channel.getLongID() == channel.getLongID());
	}

	public static boolean isMinecraftChatEnabled(DiscordPlayer dp) {
		return isMinecraftChatEnabled(dp.getDiscordID());
	}

	public static boolean isMinecraftChatEnabled(String did) { // Don't load the player data just for this
		return lastmsgPerUser.stream()
				.anyMatch(lmd -> ((IPrivateChannel) lmd.channel).getRecipient().getStringID().equals(did));
	}
}
