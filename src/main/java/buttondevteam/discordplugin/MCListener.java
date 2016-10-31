package buttondevteam.discordplugin;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.TBMCPlayerJoinEvent;

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
}
