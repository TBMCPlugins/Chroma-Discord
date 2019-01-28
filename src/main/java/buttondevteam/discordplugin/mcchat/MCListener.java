package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.player.*;
import com.earth2me.essentials.CommandSource;
import lombok.val;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.MuteStatusChangeEvent;
import net.ess3.api.events.NickChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;

class MCListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
		if (e.getResult() != Result.ALLOWED)
			return;
		MCChatUtils.ConnectedSenders.values().stream().flatMap(v -> v.values().stream()) //Only private mcchat should be in ConnectedSenders
				.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
				.ifPresent(dcp -> buttondevteam.discordplugin.listeners.MCListener.callEventExcludingSome(new PlayerQuitEvent(dcp, "")));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Don't show the joined message for the fake player
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
			final Player p = e.getPlayer();
			DiscordPlayer dp = e.GetPlayer().getAs(DiscordPlayer.class);
			if (dp != null) {
				val user = DiscordPlugin.dc.getUserByID(Long.parseLong(dp.getDiscordID()));
				MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID(),
						new DiscordPlayerSender(user, user.getOrCreatePMChannel(), p));
				MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID(),
						new DiscordPlayerSender(user, DiscordPlugin.chatchannel, p)); //Stored per-channel
			}
			if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().get())) {
				IUser user = DiscordPlugin.dc
						.getUserByID(Long.parseLong(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().get())));
				p.sendMessage("§bTo connect with the Discord account @" + user.getName() + "#" + user.getDiscriminator()
						+ " do /discord accept");
				p.sendMessage("§bIf it wasn't you, do /discord decline");
			}
			final String message = e.GetPlayer().PlayerName().get() + " joined the game";
			MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(message), e.getPlayer(), ChannelconBroadcast.JOINLEAVE, true);
			ChromaBot.getInstance().updatePlayerList();
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLeave(TBMCPlayerQuitEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Only care about real users
		MCChatUtils.OnlineSenders.entrySet()
				.removeIf(entry -> entry.getValue().entrySet().stream().anyMatch(p -> p.getValue().getUniqueId().equals(e.getPlayer().getUniqueId())));
		Bukkit.getScheduler().runTask(DiscordPlugin.plugin,
				() -> MCChatUtils.ConnectedSenders.values().stream().flatMap(v -> v.values().stream())
						.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
						.ifPresent(dcp -> buttondevteam.discordplugin.listeners.MCListener.callEventExcludingSome(new PlayerJoinEvent(dcp, ""))));
		Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin,
				ChromaBot.getInstance()::updatePlayerList, 5);
		final String message = e.GetPlayer().PlayerName().get() + " left the game";
		MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(message), e.getPlayer(), ChannelconBroadcast.JOINLEAVE, true);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerKick(PlayerKickEvent e) {
        /*if (!DiscordPlugin.hooked && !e.getReason().equals("The server is restarting")
                && !e.getReason().equals("Server closed")) // The leave messages errored with the previous setup, I could make it wait since I moved it here, but instead I have a special
            MCChatListener.forAllowedCustomAndAllMCChat(e.getPlayer().getName() + " left the game"); // message for this - Oh wait this doesn't even send normally because of the hook*/
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent e) {
		MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(e.getDeathMessage()), e.getEntity(), ChannelconBroadcast.DEATH, true);
	}

	@EventHandler
	public void onPlayerAFK(AfkStatusChangeEvent e) {
		final Player base = e.getAffected().getBase();
		if (e.isCancelled() || !base.isOnline())
			return;
		final String msg = base.getDisplayName()
				+ " is " + (e.getValue() ? "now" : "no longer") + " AFK.";
		MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(msg), base, ChannelconBroadcast.AFK, false);
	}

	@EventHandler
	public void onPlayerMute(MuteStatusChangeEvent e) {
		try {
			DPUtils.performNoWait(() -> {
				final IRole role = DiscordPlugin.dc.getRoleByID(164090010461667328L);
				final CommandSource source = e.getAffected().getSource();
				if (!source.isPlayer())
					return;
				final DiscordPlayer p = TBMCPlayerBase.getPlayer(source.getPlayer().getUniqueId(), TBMCPlayer.class)
						.getAs(DiscordPlayer.class);
				if (p == null) return;
				final IUser user = DiscordPlugin.dc.getUserByID(
						Long.parseLong(p.getDiscordID()));
				if (e.getValue())
					user.addRole(role);
				else
					user.removeRole(role);
				DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, (e.getValue() ? "M" : "Unm") + "uted user: " + user.getName());
			});
		} catch (DiscordException | MissingPermissionsException ex) {
			TBMCCoreAPI.SendException("Failed to give/take Muted role to player " + e.getAffected().getName() + "!",
					ex);
		}
	}

	@EventHandler
	public void onChatSystemMessage(TBMCSystemChatEvent event) {
		MCChatUtils.forAllowedMCChat(MCChatUtils.send(event.getMessage()), event);
	}

	@EventHandler
	public void onBroadcastMessage(BroadcastMessageEvent event) {
		MCChatUtils.forCustomAndAllMCChat(MCChatUtils.send(event.getMessage()), ChannelconBroadcast.BROADCAST, false);
	}

	@EventHandler
	public void onYEEHAW(TBMCYEEHAWEvent event) { //TODO: Inherit from the chat event base to have channel support
		String name = event.getSender() instanceof Player ? ((Player) event.getSender()).getDisplayName()
				: event.getSender().getName();
		//Channel channel = ChromaGamerBase.getFromSender(event.getSender()).channel().get(); - TODO
		MCChatUtils.forAllMCChat(MCChatUtils.send(name + " <:YEEHAW:" + DiscordPlugin.mainServer.getEmojiByName("YEEHAW").getStringID() + ">s"));
	}

	@EventHandler
	public void onNickChange(NickChangeEvent event) {
		MCChatUtils.updatePlayerList();
	}
}
