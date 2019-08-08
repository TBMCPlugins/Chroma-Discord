package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent;
import buttondevteam.lib.player.TBMCPlayerJoinEvent;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.User;
import discord4j.core.object.util.Snowflake;
import lombok.val;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import reactor.core.publisher.Mono;

public class MCListener implements Listener {
	@EventHandler
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().get())) {
			@SuppressWarnings("ConstantConditions") User user = DiscordPlugin.dc
				.getUserById(Snowflake.of(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().get()))).block();
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
		val memberOpt = user.asMember(DiscordPlugin.mainServer.getId()).blockOptional();
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

	@EventHandler
	public void onServerCommand(ServerCommandEvent e) {
		DiscordPlugin.Restart = !e.getCommand().equalsIgnoreCase("stop"); // The variable is always true except if stopped
	}
}
