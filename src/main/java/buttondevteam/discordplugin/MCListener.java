package buttondevteam.discordplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;

public class MCListener implements Listener {
	@EventHandler
	public void onServerCommandPreprocess(ServerCommandEvent e) {
		if (!DiscordPlugin.dc.isReady())
			return;
		try {
			if (e.getCommand().equalsIgnoreCase("stop"))
				DiscordPlugin.botchannel.sendMessage("Minecraft server shutting down!");
			else if (e.getCommand().equalsIgnoreCase("restart"))
				DiscordPlugin.botchannel.sendMessage("Minecraft server restarting");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
