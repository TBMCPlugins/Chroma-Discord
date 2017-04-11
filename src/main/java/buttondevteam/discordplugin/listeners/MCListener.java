package buttondevteam.discordplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.server.ServerCommandEvent;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.player.*;
import net.ess3.api.events.*;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Status.StatusType;

public class MCListener implements Listener {
	@EventHandler
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		final Player p = Bukkit.getPlayer(e.GetPlayer().getUUID());
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().get())) {
			IUser user = DiscordPlugin.dc
					.getUserByID(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().get()));
			p.sendMessage("§bTo connect with the Discord account @" + user.getName() + "#" + user.getDiscriminator()
					+ " do /discord accept");
			p.sendMessage("§bIf it wasn't you, do /discord decline");
		}
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				e.GetPlayer().PlayerName().get() + " joined the game");
		MCChatListener.ListC = 0;
	}

	@EventHandler
	public void onPlayerLeave(TBMCPlayerQuitEvent e) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				e.GetPlayer().PlayerName().get() + " left the game");
	}

	@EventHandler
	public void onGetInfo(TBMCPlayerGetInfoEvent e) {
		if (DiscordPlugin.SafeMode)
			return;
		DiscordPlayer dp = e.getPlayer().getAs(DiscordPlayer.class);
		/*System.out.println("dp: " + dp);
		if (dp != null)
			System.out.println("dp.did: " + dp.getDiscordID());*/
		if (dp == null || dp.getDiscordID() == null || dp.getDiscordID() == "")
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

	@EventHandler
	public void onPlayerAFK(AfkStatusChangeEvent e) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				DiscordPlugin.sanitizeString(e.getAffected().getBase().getDisplayName()) + " is "
						+ (e.getValue() ? "now" : "no longer") + " AFK.");
	}

	@EventHandler
	public void onServerCommand(ServerCommandEvent e) {
		DiscordPlugin.Restart = !e.getCommand().equalsIgnoreCase("stop"); // The variable is always true except if stopped
	}

	/*
	 * @EventHandler public void onPlayerMute(MuteStatusChangeEvent e) { e.getAffected() }
	 */
}
