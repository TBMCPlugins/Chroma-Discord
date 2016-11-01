package buttondevteam.discordplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCPlayerGetInfoEvent;
import buttondevteam.lib.TBMCPlayerJoinEvent;
import sx.blah.discord.handle.obj.IUser;

public class MCListener implements Listener {
	@EventHandler
	public void onServerCommandPreprocess(ServerCommandEvent e) {
		if (!DiscordPlugin.dc.isReady())
			return;
		try {
			if (e.getCommand().equalsIgnoreCase("stop"))
				DiscordPlugin.sendMessageToChannel(DiscordPlugin.botchannel, "Minecraft server shutting down!");
			else if (e.getCommand().equalsIgnoreCase("restart"))
				DiscordPlugin.sendMessageToChannel(DiscordPlugin.botchannel, "Minecraft server restarting");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	@EventHandler
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		final Player p = Bukkit.getPlayer(e.GetPlayer().getUuid());
		p.sendMessage("§bTo connect with the Discord account "
				+ ConnectCommand.WaitingToConnect.get(e.GetPlayer().getPlayerName()) + " do /discord accept");
		p.sendMessage("§bIf it wasn't you, do /discord decline");
	}

	@EventHandler
	public void onGetInfo(TBMCPlayerGetInfoEvent e) {
		DiscordPlayer dp = e.getPlayer().asPluginPlayer(DiscordPlayer.class);
		if (dp.getDiscordID() == null || dp.getDiscordID() == "")
			return;
		IUser user = DiscordPlugin.dc.getUserByID(dp.getDiscordID());
		e.addInfo("Discord tag: " + user.getName() + "#" + user.getDiscriminator());
		e.addInfo("Discord status: " + user.getStatus());
	}
}
