package buttondevteam.discordplugin.playerfaker.perm;

import buttondevteam.core.MainPlugin;
import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.discordplugin.playerfaker.DiscordFakePlayer;
import buttondevteam.lib.TBMCCoreAPI;
import me.lucko.luckperms.bukkit.LPBukkitBootstrap;
import me.lucko.luckperms.bukkit.LPBukkitPlugin;
import me.lucko.luckperms.bukkit.inject.dummy.DummyPermissibleBase;
import me.lucko.luckperms.bukkit.inject.permissible.LPPermissible;
import me.lucko.luckperms.bukkit.listeners.BukkitConnectionListener;
import me.lucko.luckperms.common.config.ConfigKeys;
import me.lucko.luckperms.common.locale.message.Message;
import me.lucko.luckperms.common.model.User;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.PermissionAttachment;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LPInjector implements Listener { //Disable login event for LuckPerms
	private LPBukkitPlugin plugin;
	private BukkitConnectionListener connectionListener;
	private Set<UUID> deniedLogin;
	private Field detectedCraftBukkitOfflineMode;
	private Method printCraftBukkitOfflineModeError;
	private Field PERMISSIBLE_BASE_ATTACHMENTS_FIELD;
	private Method convertAndAddAttachments;
	private Method getActive;
	private Method setOldPermissible;
	private Method getOldPermissible;

	public LPInjector(MainPlugin mp) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException {
		LPBukkitBootstrap bs = (LPBukkitBootstrap) Bukkit.getPluginManager().getPlugin("LuckPerms");
		Field field = LPBukkitBootstrap.class.getDeclaredField("plugin");
		field.setAccessible(true);
		plugin = (LPBukkitPlugin) field.get(bs);
		MCChatUtils.addStaticExcludedPlugin(PlayerLoginEvent.class, "LuckPerms");
		MCChatUtils.addStaticExcludedPlugin(PlayerQuitEvent.class, "LuckPerms");

		field = LPBukkitPlugin.class.getDeclaredField("connectionListener");
		field.setAccessible(true);
		connectionListener = (BukkitConnectionListener) field.get(plugin);
		field = connectionListener.getClass().getDeclaredField("deniedLogin");
		field.setAccessible(true);
		//noinspection unchecked
		deniedLogin = (Set<UUID>) field.get(connectionListener);
		field = connectionListener.getClass().getDeclaredField("detectedCraftBukkitOfflineMode");
		field.setAccessible(true);
		detectedCraftBukkitOfflineMode = field;
		printCraftBukkitOfflineModeError = connectionListener.getClass().getDeclaredMethod("printCraftBukkitOfflineModeError");
		printCraftBukkitOfflineModeError.setAccessible(true);

		//PERMISSIBLE_FIELD = DiscordFakePlayer.class.getDeclaredField("perm");
		//PERMISSIBLE_FIELD.setAccessible(true); //Hacking my own plugin, while we're at it
		PERMISSIBLE_BASE_ATTACHMENTS_FIELD = PermissibleBase.class.getDeclaredField("attachments");
		PERMISSIBLE_BASE_ATTACHMENTS_FIELD.setAccessible(true);

		convertAndAddAttachments = LPPermissible.class.getDeclaredMethod("convertAndAddAttachments", Collection.class);
		convertAndAddAttachments.setAccessible(true);
		getActive = LPPermissible.class.getDeclaredMethod("getActive");
		getActive.setAccessible(true);
		setOldPermissible = LPPermissible.class.getDeclaredMethod("setOldPermissible", PermissibleBase.class);
		setOldPermissible.setAccessible(true);
		getOldPermissible = LPPermissible.class.getDeclaredMethod("getOldPermissible");
		getOldPermissible.setAccessible(true);

		TBMCCoreAPI.RegisterEventsForExceptions(this, mp);
	}


	//Code copied from LuckPerms - me.lucko.luckperms.bukkit.listeners.BukkitConnectionListener
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
        /* Called when the player starts logging into the server.
           At this point, the users data should be present and loaded. */

		if (!(e.getPlayer() instanceof DiscordFakePlayer))
			return; //Normal players must be handled by the plugin

		final DiscordFakePlayer player = (DiscordFakePlayer) e.getPlayer();

		if (plugin.getConfiguration().get(ConfigKeys.DEBUG_LOGINS)) {
			plugin.getLogger().info("Processing login for " + player.getUniqueId() + " - " + player.getName());
		}

		final User user = plugin.getUserManager().getIfLoaded(player.getUniqueId());

		/* User instance is null for whatever reason. Could be that it was unloaded between asyncpre and now. */
		if (user == null) {
			deniedLogin.add(player.getUniqueId());

			if (!connectionListener.getUniqueConnections().contains(player.getUniqueId())) {

				plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getName() +
					" doesn't have data pre-loaded, they have never been processed during pre-login in this session." +
					" - denying login.");

				try {
					if ((Boolean) detectedCraftBukkitOfflineMode.get(connectionListener)) {
						printCraftBukkitOfflineModeError.invoke(connectionListener);
						e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Message.LOADING_STATE_ERROR_CB_OFFLINE_MODE.asString(plugin.getLocaleManager()));
						return;
					}
				} catch (IllegalAccessException | InvocationTargetException ex) {
					ex.printStackTrace();
				}

			} else {
				plugin.getLogger().warn("User " + player.getUniqueId() + " - " + player.getName() +
					" doesn't currently have data pre-loaded, but they have been processed before in this session." +
					" - denying login.");
			}

			e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Message.LOADING_STATE_ERROR.asString(plugin.getLocaleManager()));
			return;
		}

		// User instance is there, now we can inject our custom Permissible into the player.
		// Care should be taken at this stage to ensure that async tasks which manipulate bukkit data check that the player is still online.
		try {
			// get the existing PermissibleBase held by the player
			PermissibleBase oldPermissible = player.getPerm();

			// Make a new permissible for the user
			LPPermissible lpPermissible = new LPPermissible(player, user, plugin);

			// Inject into the player
			inject(player, lpPermissible, oldPermissible);

		} catch (Throwable t) {
			plugin.getLogger().warn("Exception thrown when setting up permissions for " +
				player.getUniqueId() + " - " + player.getName() + " - denying login.");
			t.printStackTrace();

			e.disallow(PlayerLoginEvent.Result.KICK_OTHER, Message.LOADING_SETUP_ERROR.asString(plugin.getLocaleManager()));
			return;
		}

		plugin.refreshAutoOp(player, true);
	}

	// Wait until the last priority to unload, so plugins can still perform permission checks on this event
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent e) {
		if (!(e.getPlayer() instanceof DiscordFakePlayer))
			return;

		final DiscordFakePlayer player = (DiscordFakePlayer) e.getPlayer();

		connectionListener.handleDisconnect(player.getUniqueId());

		// perform unhooking from bukkit objects 1 tick later.
		// this allows plugins listening after us on MONITOR to still have intact permissions data
		this.plugin.getBootstrap().getServer().getScheduler().runTaskLaterAsynchronously(this.plugin.getBootstrap(), () -> {
			// Remove the custom permissible
			try {
				uninject(player, true);
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			// Handle auto op
			if (this.plugin.getConfiguration().get(ConfigKeys.AUTO_OP)) {
				player.setOp(false);
			}

			// remove their contexts cache
			this.plugin.getContextManager().onPlayerQuit(player);
		}, 1L);
	}

	//me.lucko.luckperms.bukkit.inject.permissible.PermissibleInjector
	private void inject(DiscordFakePlayer player, LPPermissible newPermissible, PermissibleBase oldPermissible) throws IllegalAccessException, InvocationTargetException {

		// seems we have already injected into this player.
		if (oldPermissible instanceof LPPermissible) {
			throw new IllegalStateException("LPPermissible already injected into player " + player.toString());
		}

		// Move attachments over from the old permissible

		//noinspection unchecked
		List<PermissionAttachment> attachments = (List<PermissionAttachment>) PERMISSIBLE_BASE_ATTACHMENTS_FIELD.get(oldPermissible);

		convertAndAddAttachments.invoke(newPermissible, attachments);
		attachments.clear();
		oldPermissible.clearPermissions();

		// Setup the new permissible
		((AtomicBoolean) getActive.invoke(newPermissible)).set(true);
		setOldPermissible.invoke(newPermissible, oldPermissible);

		// inject the new instance
		player.setPerm(newPermissible);
	}

	private void uninject(DiscordFakePlayer player, boolean dummy) throws Exception {

		// gets the players current permissible.
		PermissibleBase permissible = player.getPerm();

		// only uninject if the permissible was a luckperms one.
		if (permissible instanceof LPPermissible) {
			LPPermissible lpPermissible = ((LPPermissible) permissible);

			// clear all permissions
			lpPermissible.clearPermissions();

			// set to inactive
			((AtomicBoolean) getActive.invoke(lpPermissible)).set(false);

			// handle the replacement permissible.
			if (dummy) {
				// just inject a dummy class. this is used when we know the player is about to quit the server.
				player.setPerm(DummyPermissibleBase.INSTANCE);

			} else {
				PermissibleBase newPb = (PermissibleBase) getOldPermissible.invoke(lpPermissible);
				if (newPb == null) {
					newPb = new PermissibleBase(player);
				}

				player.setPerm(newPb);
			}
		}
	}
}
