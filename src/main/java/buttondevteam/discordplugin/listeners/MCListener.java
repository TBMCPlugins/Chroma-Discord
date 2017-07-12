package buttondevteam.discordplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;

import com.earth2me.essentials.CommandSource;

import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlayerSender;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.player.*;
import lombok.val;
import net.ess3.api.events.*;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;

public class MCListener implements Listener {
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Don't show the joined message for the fake player
		final Player p = e.getPlayer();
		DiscordPlayer dp = e.GetPlayer().getAs(DiscordPlayer.class);
		if (dp != null) {
			val user = DiscordPlugin.dc.getUserByID(Long.parseLong(dp.getDiscordID()));
			MCChatListener.OnlineSenders.put(dp.getDiscordID(),
					new DiscordPlayerSender(user, user.getOrCreatePMChannel(), p));
			MCChatListener.OnlineSenders.put("P" + dp.getDiscordID(),
					new DiscordPlayerSender(user, DiscordPlugin.chatchannel, p));
			MCChatListener.ConnectedSenders.values().stream()
					.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
					.ifPresent(dcp -> Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(dcp, "")));
		}
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().get())) {
			IUser user = DiscordPlugin.dc
					.getUserByID(Long.parseLong(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().get())));
			p.sendMessage("§bTo connect with the Discord account @" + user.getName() + "#" + user.getDiscriminator()
					+ " do /discord accept");
			p.sendMessage("§bIf it wasn't you, do /discord decline");
		}
		MCChatListener.sendSystemMessageToChat(e.GetPlayer().PlayerName().get() + " joined the game");
		MCChatListener.ListC = 0;
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLeave(TBMCPlayerQuitEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Only care about real users
		MCChatListener.OnlineSenders.entrySet()
				.removeIf(entry -> entry.getValue().getUniqueId().equals(e.getPlayer().getUniqueId()));
		MCChatListener.ConnectedSenders.values().stream()
				.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
				.ifPresent(dcp -> Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(dcp, "")));
		MCChatListener.sendSystemMessageToChat(e.GetPlayer().PlayerName().get() + " left the game");
	}

	@EventHandler
	public void onGetInfo(TBMCPlayerGetInfoEvent e) {
		if (DiscordPlugin.SafeMode)
			return;
		DiscordPlayer dp = e.getPlayer().getAs(DiscordPlayer.class);
		if (dp == null || dp.getDiscordID() == null || dp.getDiscordID() == "")
			return;
		IUser user = DiscordPlugin.dc.getUserByID(Long.parseLong(dp.getDiscordID()));
		e.addInfo("Discord tag: " + user.getName() + "#" + user.getDiscriminator());
		e.addInfo(user.getPresence().getStatus().toString());
		if (user.getPresence().getPlayingText().isPresent())
			e.addInfo("Playing " + user.getPresence().getPlayingText().get());
		else if (user.getPresence().getStreamingUrl().isPresent())
			e.addInfo("Streaming " + user.getPresence().getStreamingUrl().get());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent e) {
		MCChatListener.sendSystemMessageToChat(e.getDeathMessage());
	}

	@EventHandler
	public void onPlayerAFK(AfkStatusChangeEvent e) {
		if (e.isCancelled() || !e.getAffected().getBase().isOnline())
			return;
		MCChatListener.sendSystemMessageToChat(DiscordPlugin.sanitizeString(e.getAffected().getBase().getDisplayName())
				+ " is " + (e.getValue() ? "now" : "no longer") + " AFK.");
	}

	@EventHandler
	public void onServerCommand(ServerCommandEvent e) {
		DiscordPlugin.Restart = !e.getCommand().equalsIgnoreCase("stop"); // The variable is always true except if stopped
	}

	@EventHandler
	public void onPlayerMute(MuteStatusChangeEvent e) {
		try {
			DiscordPlugin.perform(() -> {
				final IRole role = DiscordPlugin.dc.getRoleByID(164090010461667328L);
				final CommandSource source = e.getAffected().getSource();
				if (!source.isPlayer())
					return;
				final IUser user = DiscordPlugin.dc.getUserByID(
						Long.parseLong(TBMCPlayerBase.getPlayer(source.getPlayer().getUniqueId(), TBMCPlayer.class)
								.getAs(DiscordPlayer.class).getDiscordID())); // TODO: Use long
				if (e.getValue())
					user.addRole(role);
				else
					user.removeRole(role);
			});
		} catch (DiscordException | MissingPermissionsException ex) {
			TBMCCoreAPI.SendException("Failed to give/take Muted role to player " + e.getAffected().getName() + "!",
					ex);
		}
	}
}
