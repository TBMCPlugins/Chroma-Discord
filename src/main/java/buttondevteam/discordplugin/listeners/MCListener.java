package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import sx.blah.discord.handle.obj.IUser;

import java.util.Arrays;
import java.util.logging.Level;

public class MCListener implements Listener {
    @EventHandler
    public void onGetInfo(TBMCPlayerGetInfoEvent e) {
        if (DiscordPlugin.SafeMode)
            return;
        DiscordPlayer dp = e.getPlayer().getAs(DiscordPlayer.class);
        if (dp == null || dp.getDiscordID() == null || dp.getDiscordID().equals(""))
            return;
        IUser user = DiscordPlugin.dc.getUserByID(Long.parseLong(dp.getDiscordID()));
        e.addInfo("Discord tag: " + user.getName() + "#" + user.getDiscriminator());
        e.addInfo(user.getPresence().getStatus().toString());
        if (user.getPresence().getActivity().isPresent() && user.getPresence().getText().isPresent())
            e.addInfo(user.getPresence().getActivity().get() + ": " + user.getPresence().getText().get());
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent e) {
        DiscordPlugin.Restart = !e.getCommand().equalsIgnoreCase("stop"); // The variable is always true except if stopped
    }

	private static final String[] EXCLUDED_PLUGINS = {"ProtocolLib", "LibsDisguises", "JourneyMapServer"}; //TODO: Make configurable

	public static void callEventExcludingSome(Event event) {
		callEventExcluding(event, false, EXCLUDED_PLUGINS);
	}

	/**
	 * Calls an event with the given details.
	 * <p>
	 * This method only synchronizes when the event is not asynchronous.
	 *
	 * @param event   Event details
	 * @param only    Flips the operation and <b>includes</b> the listed plugins
	 * @param plugins The plugins to exclude. Not case sensitive.
	 */
	public static void callEventExcluding(Event event, boolean only, String... plugins) { // Copied from Spigot-API and modified a bit
		if (event.isAsynchronous()) {
			if (Thread.holdsLock(Bukkit.getPluginManager())) {
				throw new IllegalStateException(
						event.getEventName() + " cannot be triggered asynchronously from inside synchronized code.");
			}
			if (Bukkit.getServer().isPrimaryThread()) {
				throw new IllegalStateException(
						event.getEventName() + " cannot be triggered asynchronously from primary server thread.");
			}
			fireEventExcluding(event, only, plugins);
		} else {
			synchronized (Bukkit.getPluginManager()) {
				fireEventExcluding(event, only, plugins);
			}
		}
	}

	private static void fireEventExcluding(Event event, boolean only, String... plugins) {
		HandlerList handlers = event.getHandlers(); // Code taken from SimplePluginManager in Spigot-API
		RegisteredListener[] listeners = handlers.getRegisteredListeners();
		val server = Bukkit.getServer();

		for (RegisteredListener registration : listeners) {
			if (!registration.getPlugin().isEnabled()
					|| Arrays.stream(plugins).anyMatch(p -> only ^ p.equalsIgnoreCase(registration.getPlugin().getName())))
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
