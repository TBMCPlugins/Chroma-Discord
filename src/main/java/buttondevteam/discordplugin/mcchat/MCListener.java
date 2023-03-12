package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.*;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import buttondevteam.lib.player.TBMCYEEHAWEvent;
import com.earth2me.essentials.CommandSource;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Role;
import lombok.val;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.MuteStatusChangeEvent;
import net.ess3.api.events.NickChangeEvent;
import net.ess3.api.events.VanishStatusChangeEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.TabCompleteEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

class MCListener implements Listener {
	private final MinecraftChatModule module;
	private final ConfigData<Mono<Role>> muteRole;

	public MCListener(MinecraftChatModule module) {
		this.module = module;
		muteRole = DPUtils.roleData(module.getConfig(), "muteRole", "Muted");
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLogin(PlayerLoginEvent e) {
		if (e.getResult() != Result.ALLOWED)
			return;
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return;
		var dcp = MCChatUtils.LoggedInPlayers.get(e.getPlayer().getUniqueId());
		if (dcp != null)
			MCChatUtils.callLogoutEvent(dcp, false);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Don't show the joined message for the fake player
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
			final Player p = e.getPlayer();
			DiscordPlayer dp = TBMCPlayerBase.getPlayer(p.getUniqueId(), TBMCPlayer.class).getAs(DiscordPlayer.class);
			if (dp != null) {
				DiscordPlugin.dc.getUserById(Snowflake.of(dp.getDiscordID())).flatMap(user -> user.getPrivateChannel().flatMap(chan -> module.chatChannelMono().flatMap(cc -> {
					MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID(),
						DiscordPlayerSender.create(user, chan, p, module));
					MCChatUtils.addSender(MCChatUtils.OnlineSenders, dp.getDiscordID(),
						DiscordPlayerSender.create(user, cc, p, module)); //Stored per-channel
					return Mono.empty();
				}))).subscribe();
			}
			final String message = e.getJoinMessage();
			if (message != null && message.trim().length() > 0)
				MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(message), e.getPlayer(), ChannelconBroadcast.JOINLEAVE, true).subscribe();
			ChromaBot.getInstance().updatePlayerList();
		});
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerLeave(PlayerQuitEvent e) {
		if (e.getPlayer() instanceof DiscordConnectedPlayer)
			return; // Only care about real users
		MCChatUtils.OnlineSenders.entrySet()
			.removeIf(entry -> entry.getValue().entrySet().stream().anyMatch(p -> p.getValue().getUniqueId().equals(e.getPlayer().getUniqueId())));
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin,
			() -> Optional.ofNullable(MCChatUtils.LoggedInPlayers.get(e.getPlayer().getUniqueId())).ifPresent(MCChatUtils::callLoginEvents));
		Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin,
			ChromaBot.getInstance()::updatePlayerList, 5);
		final String message = e.getQuitMessage();
		if (message != null && message.trim().length() > 0)
			MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(message), e.getPlayer(), ChannelconBroadcast.JOINLEAVE, true).subscribe();
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerKick(PlayerKickEvent e) {
        /*if (!DiscordPlugin.hooked && !e.getReason().equals("The server is restarting")
                && !e.getReason().equals("Server closed")) // The leave messages errored with the previous setup, I could make it wait since I moved it here, but instead I have a special
            MCChatListener.forAllowedCustomAndAllMCChat(e.getPlayer().getName() + " left the game"); // message for this - Oh wait this doesn't even send normally because of the hook*/
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerDeath(PlayerDeathEvent e) {
		MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(e.getDeathMessage()), e.getEntity(), ChannelconBroadcast.DEATH, true).subscribe();
	}

	@EventHandler
	public void onPlayerAFK(AfkStatusChangeEvent e) {
		final Player base = e.getAffected().getBase();
		if (e.isCancelled() || !base.isOnline())
			return;
		final String msg = base.getDisplayName()
			+ " is " + (e.getValue() ? "now" : "no longer") + " AFK.";
		MCChatUtils.forAllowedCustomAndAllMCChat(MCChatUtils.send(msg), base, ChannelconBroadcast.AFK, false).subscribe();
	}

	@EventHandler
	public void onPlayerMute(MuteStatusChangeEvent e) {
		final Mono<Role> role = muteRole.get();
		if (role == null) return;
		final CommandSource source = e.getAffected().getSource();
		if (!source.isPlayer())
			return;
		final DiscordPlayer p = TBMCPlayerBase.getPlayer(source.getPlayer().getUniqueId(), TBMCPlayer.class)
			.getAs(DiscordPlayer.class);
		if (p == null) return;
		DPUtils.ignoreError(DiscordPlugin.dc.getUserById(Snowflake.of(p.getDiscordID()))
			.flatMap(user -> user.asMember(DiscordPlugin.mainServer.getId()))
			.flatMap(user -> role.flatMap(r -> {
				if (e.getValue())
					user.addRole(r.getId());
				else
					user.removeRole(r.getId());
				val modlog = module.modlogChannel.get();
				String msg = (e.getValue() ? "M" : "Unm") + "uted user: " + user.getUsername() + "#" + user.getDiscriminator();
				module.log(msg);
				if (modlog != null)
					return modlog.flatMap(ch -> ch.createMessage(msg));
				return Mono.empty();
			}))).subscribe();
	}

	@EventHandler
	public void onChatSystemMessage(TBMCSystemChatEvent event) {
		MCChatUtils.forAllowedMCChat(MCChatUtils.send(event.getMessage()), event).subscribe();
	}

	@EventHandler
	public void onBroadcastMessage(BroadcastMessageEvent event) {
		event.getRecipients().removeIf(sender -> sender instanceof DiscordSenderBase);
		MCChatUtils.forCustomAndAllMCChat(MCChatUtils.send(event.getMessage()), ChannelconBroadcast.BROADCAST, false).subscribe();
	}

	@EventHandler
	public void onYEEHAW(TBMCYEEHAWEvent event) { //TODO: Inherit from the chat event base to have channel support
		String name = event.getSender() instanceof Player ? ((Player) event.getSender()).getDisplayName()
			: event.getSender().getName();
		//Channel channel = ChromaGamerBase.getFromSender(event.getSender()).channel().get(); - TODO
		DiscordPlugin.mainServer.getEmojis().filter(e -> "YEEHAW".equals(e.getName()))
			.take(1).singleOrEmpty().map(Optional::of).defaultIfEmpty(Optional.empty()).flatMap(yeehaw ->
			MCChatUtils.forPublicPrivateChat(MCChatUtils.send(name + (yeehaw.map(guildEmoji -> " <:YEEHAW:" + guildEmoji.getId().asString() + ">s").orElse(" YEEHAWs"))))).subscribe();
	}

	@EventHandler
	public void onNickChange(NickChangeEvent event) {
		MCChatUtils.updatePlayerList();
	}

	@EventHandler
	public void onTabComplete(TabCompleteEvent event) {
		int i = event.getBuffer().lastIndexOf(' ');
		String t = event.getBuffer().substring(i + 1); //0 if not found
		if (!t.startsWith("@"))
			return;
		String token = t.substring(1);
		val x = DiscordPlugin.mainServer.getMembers()
			.flatMap(m -> Flux.just(m.getUsername(), m.getNickname().orElse("")))
			.filter(s -> s.startsWith(token))
			.map(s -> "@" + s)
			.doOnNext(event.getCompletions()::add).blockLast();
	}

	@EventHandler
	public void onCommandSend(PlayerCommandSendEvent event) {
		event.getCommands().add("g");
	}

	@EventHandler
	public void onVanish(VanishStatusChangeEvent event) {
		if (event.isCancelled()) return;
		Bukkit.getScheduler().runTask(DiscordPlugin.plugin, MCChatUtils::updatePlayerList);
	}
}
