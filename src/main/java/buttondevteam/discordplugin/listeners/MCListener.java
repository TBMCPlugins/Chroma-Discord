package buttondevteam.discordplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.server.ServerCommandEvent;

import com.earth2me.essentials.CommandSource;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.player.*;
import net.ess3.api.events.*;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;

public class MCListener implements Listener {
	@EventHandler
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		final Player p = Bukkit.getPlayer(e.GetPlayer().getUUID());
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().getOrDefault(null))) {
			IUser user = DiscordPlugin.dc.getUserByID(
					Long.parseLong(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().getOrDefault(null))));
			p.sendMessage("§bTo connect with the Discord account @" + user.getName() + "#" + user.getDiscriminator()
					+ " do /discord accept");
			p.sendMessage("§bIf it wasn't you, do /discord decline");
		}
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				e.GetPlayer().PlayerName().getOrDefault(null) + " joined the game");
		MCChatListener.ListC = 0;
	}

	@EventHandler
	public void onPlayerLeave(TBMCPlayerQuitEvent e) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				e.GetPlayer().PlayerName().getOrDefault(null) + " left the game");
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
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel, e.getDeathMessage());
	}

	@EventHandler
	public void onPlayerAFK(AfkStatusChangeEvent e) {
		if (e.isCancelled())
			return;
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				DiscordPlugin.sanitizeString(e.getAffected().getBase().getDisplayName()) + " is "
						+ (e.getValue() ? "now" : "no longer") + " AFK.");
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
