package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.util.DPState;
import buttondevteam.lib.ScheduledServerRestartEvent;
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import reactor.core.publisher.Mono;

public class MCListener implements Listener {
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent e) {
		if (ConnectCommand.WaitingToConnect.containsKey(e.getPlayer().getName())) {
			@SuppressWarnings("ConstantConditions") User user = DiscordPlugin.dc
				.getUserById(Snowflake.of(ConnectCommand.WaitingToConnect.get(e.getPlayer().getName()))).block();
			if (user == null) return;
			e.getPlayer().sendMessage("§bTo connect with the Discord account @" + user.getUsername() + "#" + user.getDiscriminator()
				+ " do /discord accept");
			e.getPlayer().sendMessage("§bIf it wasn't you, do /discord decline");
		}
	}

	@EventHandler
	public void onGetInfo(TBMCPlayerGetInfoEvent e) {
		if (DiscordPlugin.SafeMode)
			return;
		DiscordPlayer dp = e.getPlayer().getAs(DiscordPlayer.class);
		if (dp == null || dp.getDiscordID() == null || dp.getDiscordID().equals(""))
			return;
		val userOpt = DiscordPlugin.dc.getUserById(Snowflake.of(dp.getDiscordID())).onErrorResume(t -> Mono.empty()).blockOptional();
		if (!userOpt.isPresent()) return;
		User user = userOpt.get();
		e.addInfo("Discord tag: " + user.getUsername() + "#" + user.getDiscriminator());
		val memberOpt = user.asMember(DiscordPlugin.mainServer.getId()).onErrorResume(t -> Mono.empty()).blockOptional();
		if (!memberOpt.isPresent()) return;
		Member member = memberOpt.get();
		val prOpt = member.getPresence().blockOptional();
		if (!prOpt.isPresent()) return;
		val pr = prOpt.get();
		e.addInfo(pr.getStatus().toString());
		if (pr.getActivity().isPresent()) {
			val activity = pr.getActivity().get();
			e.addInfo(activity.getType() + ": " + activity.getName());
		}
	}

	/*@EventHandler
	public void onCommandPreprocess(TBMCCommandPreprocessEvent e) {
		if (e.getMessage().equalsIgnoreCase("/stop"))
			MinecraftChatModule.state = DPState.STOPPING_SERVER;
		else if (e.getMessage().equalsIgnoreCase("/restart"))
			MinecraftChatModule.state = DPState.RESTARTING_SERVER;
	}*/

	@EventHandler //We don't really need this with the logger stuff but hey
	public void onScheduledRestart(ScheduledServerRestartEvent e) {
		MinecraftChatModule.state = DPState.RESTARTING_SERVER;
	}
}
