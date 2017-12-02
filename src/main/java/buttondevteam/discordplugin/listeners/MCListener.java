package buttondevteam.discordplugin.listeners;

import java.util.Arrays;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;

import com.earth2me.essentials.CommandSource;

import buttondevteam.discordplugin.ChromaBot;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlayerSender;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.player.*;
import lombok.val;
import net.ess3.api.events.*;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;

public class MCListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
		if (e.getResult() != Result.ALLOWED)
			return;
		MCChatListener.ConnectedSenders.values().stream()
				.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
				.ifPresent(dcp -> callEventExcludingSome(new PlayerQuitEvent(dcp, "")));
	}

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
		}
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().get())) {
			IUser user = DiscordPlugin.dc
					.getUserByID(Long.parseLong(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().get())));
			p.sendMessage("§bTo connect with the Discord account @" + user.getName() + "#" + user.getDiscriminator()
					+ " do /discord accept");
			p.sendMessage("§bIf it wasn't you, do /discord decline");
		}
		if (!DiscordPlugin.hooked)
			MCChatListener.sendSystemMessageToChat(e.GetPlayer().PlayerName().get() + " joined the game");
		MCChatListener.ListC = 0;
		ChromaBot.getInstance().updatePlayerList();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLeave(TBMCPlayerQuitEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Only care about real users
		MCChatListener.OnlineSenders.entrySet()
				.removeIf(entry -> entry.getValue().getUniqueId().equals(e.getPlayer().getUniqueId()));
		Bukkit.getScheduler().runTask(DiscordPlugin.plugin,
				() -> MCChatListener.ConnectedSenders.values().stream()
						.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
						.ifPresent(dcp -> callEventExcludingSome(new PlayerJoinEvent(dcp, ""))));
		if (!DiscordPlugin.hooked)
			MCChatListener.sendSystemMessageToChat(e.GetPlayer().PlayerName().get() + " left the game");
		Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin,
				ChromaBot.getInstance()::updatePlayerList, 5);
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
		if (!DiscordPlugin.hooked)
			MCChatListener.sendSystemMessageToChat(e.getDeathMessage());
	}

	@EventHandler
	public void onPlayerAFK(AfkStatusChangeEvent e) {
		if (e.isCancelled() || !e.getAffected().getBase().isOnline())
			return;
		MCChatListener.sendSystemMessageToChat(DPUtils.sanitizeString(e.getAffected().getBase().getDisplayName())
				+ " is " + (e.getValue() ? "now" : "no longer") + " AFK.");
	}

	@EventHandler
	public void onServerCommand(ServerCommandEvent e) {
		DiscordPlugin.Restart = !e.getCommand().equalsIgnoreCase("stop"); // The variable is always true except if stopped
	}

	@EventHandler
	public void onPlayerMute(MuteStatusChangeEvent e) {
		try {
			DPUtils.performNoWait(() -> {
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

	@EventHandler
	public void onChatSystemMessage(TBMCSystemChatEvent event) {
		MCChatListener.sendSystemMessageToChat(event);
	}

	@EventHandler
	public void onBroadcastMessage(BroadcastMessageEvent event) {
		MCChatListener.sendSystemMessageToChat(event.getMessage());
	}

	private static final String[] EXCLUDED_PLUGINS = { "ProtocolLib", "LibsDisguises" };

	public static void callEventExcludingSome(Event event) {
		callEventExcluding(event, EXCLUDED_PLUGINS);
	}

	/**
	 * Calls an event with the given details.
	 * <p>
	 * This method only synchronizes when the event is not asynchronous.
	 *
	 * @param event
	 *            Event details
	 * @param plugins
	 *            The plugins to exclude. Not case sensitive.
	 */
	private static void callEventExcluding(Event event, String... plugins) { // Copied from Spigot-API and modified a bit
		if (event.isAsynchronous()) {
			if (Thread.holdsLock(Bukkit.getPluginManager())) {
				throw new IllegalStateException(
						event.getEventName() + " cannot be triggered asynchronously from inside synchronized code.");
			}
			if (Bukkit.getServer().isPrimaryThread()) {
				throw new IllegalStateException(
						event.getEventName() + " cannot be triggered asynchronously from primary server thread.");
			}
			fireEventExcluding(event, plugins);
		} else {
			synchronized (Bukkit.getPluginManager()) {
				fireEventExcluding(event, plugins);
			}
		}
	}

	private static void fireEventExcluding(Event event, String... plugins) {
		HandlerList handlers = event.getHandlers(); // Code taken from SimplePluginManager in Spigot-API
		RegisteredListener[] listeners = handlers.getRegisteredListeners();
		val server = Bukkit.getServer();

		for (RegisteredListener registration : listeners) {
			if (!registration.getPlugin().isEnabled()
					|| Arrays.stream(plugins).anyMatch(p -> p.equalsIgnoreCase(registration.getPlugin().getName())))
				continue; // Modified to exclude plugins

			try {
				registration.callEvent(event);
			} catch (AuthorNagException ex) {
				Plugin plugin = registration.getPlugin();

				if (plugin.isNaggable()) {
					plugin.setNaggable(false);

					server.getLogger().log(Level.SEVERE,
							String.format("Nag author(s): '%s' of '%s' about the following: %s",
									plugin.getDescription().getAuthors(), plugin.getDescription().getFullName(),
									ex.getMessage()));
				}
			} catch (Throwable ex) {
				server.getLogger().log(Level.SEVERE, "Could not pass event " + event.getEventName() + " to "
						+ registration.getPlugin().getDescription().getFullName(), ex);
			}
		}
	}
}
