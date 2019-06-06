package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.*;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.player.*;
import com.earth2me.essentials.CommandSource;
import discord4j.core.object.entity.Role;
import discord4j.core.object.util.Snowflake;
import lombok.RequiredArgsConstructor;
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
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.BroadcastMessageEvent;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;

@RequiredArgsConstructor
class MCListener implements Listener {
	private final MinecraftChatModule module;

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
		if (e.getResult() != Result.ALLOWED)
			return;
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return;
		MCChatUtils.ConnectedSenders.values().stream().flatMap(v -> v.values().stream()) //Only private mcchat should be in ConnectedSenders
			.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
			.ifPresent(dcp -> MCChatUtils.callLogoutEvent(dcp, false));
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Don't show the joined message for the fake player
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
			final Player p = e.getPlayer();
			DiscordPlayer dp = e.GetPlayer().getAs(DiscordPlayer.class);
			if (dp != null) {
				val user = DiscordPlugin.dc.getUserById(Snowflake.of(dp.getDiscordID())).block();
				MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID(),
					new DiscordPlayerSender(user, Objects.requireNonNull(user).getPrivateChannel().block(), p)); //TODO: Don't block
				MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID(),
					new DiscordPlayerSender(user, module.chatChannelMono().block(), p)); //Stored per-channel
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
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin,
			() -> MCChatUtils.ConnectedSenders.values().stream().flatMap(v -> v.values().stream())
				.filter(s -> s.getUniqueId().equals(e.getPlayer().getUniqueId())).findAny()
				.ifPresent(MCChatUtils::callLoginEvents));
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

	private ConfigData<Mono<Role>> muteRole() {
		return DPUtils.roleData(module.getConfig(), "muteRole", "Muted");
	}

	@EventHandler
	public void onPlayerMute(MuteStatusChangeEvent e) {
		final Mono<Role> role = muteRole().get();
		if (role == null) return;
		final CommandSource source = e.getAffected().getSource();
		if (!source.isPlayer())
			return;
		final DiscordPlayer p = TBMCPlayerBase.getPlayer(source.getPlayer().getUniqueId(), TBMCPlayer.class)
			.getAs(DiscordPlayer.class);
		if (p == null) return;
		DiscordPlugin.dc.getUserById(Snowflake.of(p.getDiscordID()))
			.flatMap(user -> user.asMember(DiscordPlugin.mainServer.getId()))
			.flatMap(user -> role.flatMap(r -> {
				if (e.getValue())
					user.addRole(r.getId());
				else
					user.removeRole(r.getId());
				val modlog = module.modlogChannel().get();
				String msg = (e.getValue() ? "M" : "Unm") + "uted user: " + user.getUsername() + "#" + user.getDiscriminator();
				DPUtils.getLogger().info(msg);
				if (modlog != null)
					return modlog.flatMap(ch -> ch.createMessage(msg));
				return Mono.empty();
			})).subscribe();
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
		DiscordPlugin.mainServer.getEmojis().filter(e -> "YEEHAW".equals(e.getName()))
			.take(1).singleOrEmpty().map(Optional::of).defaultIfEmpty(Optional.empty()).subscribe(yeehaw ->
			MCChatUtils.forAllMCChat(MCChatUtils.send(name + (yeehaw.map(guildEmoji -> " <:YEEHAW:" + guildEmoji.getId().asString() + ">s").orElse(" YEEHAWs")))));
	}

	@EventHandler
	public void onNickChange(NickChangeEvent event) {
		MCChatUtils.updatePlayerList();
	}
}
