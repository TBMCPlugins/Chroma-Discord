package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.player.TBMCPlayer;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;

public class MCChatPrivate {

	/**
	 * Used for messages in PMs (mcchat).
	 */
	static ArrayList<MCChatUtils.LastMsgData> lastmsgPerUser = new ArrayList<>();

	public static boolean privateMCChat(MessageChannel channel, boolean start, User user, DiscordPlayer dp) {
		TBMCPlayer mcp = dp.getAs(TBMCPlayer.class);
		if (mcp != null) { // If the accounts aren't connected, can't make a connected sender
			val p = Bukkit.getPlayer(mcp.getUUID());
			val op = Bukkit.getOfflinePlayer(mcp.getUUID());
			val mcm = ComponentManager.getIfEnabled(MinecraftChatModule.class);
			if (start) {
				val sender = new DiscordConnectedPlayer(user, channel, mcp.getUUID(), op.getName(), mcm);
				MCChatUtils.addSender(MCChatUtils.ConnectedSenders, user, sender);
				if (p == null)// Player is offline - If the player is online, that takes precedence
					callEventSync(new PlayerJoinEvent(sender, ""));
			} else {
				val sender = MCChatUtils.removeSender(MCChatUtils.ConnectedSenders, channel.getId(), user);
				if (p == null)// Player is offline - If the player is online, that takes precedence
					callEventSync(new PlayerQuitEvent(sender, ""));
			}
		} // ---- PermissionsEx warning is normal on logout ----
		if (!start)
			MCChatUtils.lastmsgfromd.remove(channel.getId().asLong());
		return start //
			? lastmsgPerUser.add(new MCChatUtils.LastMsgData((TextChannel) channel, user)) // Doesn't support group DMs
			: lastmsgPerUser.removeIf(lmd -> lmd.channel.getId().asLong() == channel.getId().asLong());
	}

	public static boolean isMinecraftChatEnabled(DiscordPlayer dp) {
		return isMinecraftChatEnabled(dp.getDiscordID());
	}

	public static boolean isMinecraftChatEnabled(String did) { // Don't load the player data just for this
		return lastmsgPerUser.stream()
			.anyMatch(lmd -> ((PrivateChannel) lmd.channel)
				.getRecipientIds().stream().anyMatch(u -> u.asString().equals(did)));
	}

	public static void logoutAll() {
		for (val entry : MCChatUtils.ConnectedSenders.entrySet())
			for (val valueEntry : entry.getValue().entrySet())
				if (MCChatUtils.getSender(MCChatUtils.OnlineSenders, valueEntry.getKey(), valueEntry.getValue().getUser()) == null) //If the player is online then the fake player was already logged out
					MCChatUtils.callEventExcludingSome(new PlayerQuitEvent(valueEntry.getValue(), "")); //This is sync
		MCChatUtils.ConnectedSenders.clear();
	}

	private static void callEventSync(Event event) {
		Bukkit.getScheduler().runTask(DiscordPlugin.plugin, () -> MCChatUtils.callEventExcludingSome(event));
	}
}
