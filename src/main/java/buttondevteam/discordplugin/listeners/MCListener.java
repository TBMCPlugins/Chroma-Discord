package buttondevteam.discordplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCPlayerGetInfoEvent;
import buttondevteam.lib.TBMCPlayerJoinEvent;
import buttondevteam.lib.TBMCPlayerQuitEvent;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Status.StatusType;

public class MCListener implements Listener {
	@EventHandler
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		final Player p = Bukkit.getPlayer(e.GetPlayer().getUuid());
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().getPlayerName())) {
			p.sendMessage("§bTo connect with the Discord account "
					+ ConnectCommand.WaitingToConnect.get(e.GetPlayer().getPlayerName()) + " do /discord accept");
			p.sendMessage("§bIf it wasn't you, do /discord decline");
		}
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				e.GetPlayer().getPlayerName() + " joined the game");
	}

	@EventHandler
	public void onPlayerLeave(TBMCPlayerQuitEvent e) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel, e.GetPlayer().getPlayerName() + " left the game");
	}

	@EventHandler
	public void onGetInfo(TBMCPlayerGetInfoEvent e) {
		DiscordPlayer dp = e.getPlayer().asPluginPlayer(DiscordPlayer.class);
		if (dp.getDiscordID() == null || dp.getDiscordID() == "")
			return;
		IUser user = DiscordPlugin.dc.getUserByID(dp.getDiscordID());
		e.addInfo("Discord tag: " + user.getName() + "#" + user.getDiscriminator());
		if (!user.getStatus().getType().equals(StatusType.NONE)) {
			if (user.getStatus().getType().equals(StatusType.GAME))
				e.addInfo("Discord status: Playing " + user.getStatus().getStatusMessage());
			else if (user.getStatus().getType().equals(StatusType.STREAM))
				e.addInfo("Discord status: Streaming " + user.getStatus().getStatusMessage() + " - "
						+ user.getStatus().getUrl());
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent e) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel, e.getDeathMessage());
	}
}
