package buttondevteam.discordplugin.listeners;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCDebugMessageEvent;

public class DebugMessageListener implements Listener{
	@EventHandler
	public void onDebugMessage(TBMCDebugMessageEvent e) {
		SendMessage(e.getDebugMessage());
		e.setSent();
	}

	private static void SendMessage(String message) {
		if (DiscordPlugin.SafeMode)
			return;
		try {
			StringBuilder sb = new StringBuilder();
			sb.append("```").append("\n");
			if (message.length() > 2000)
				message = message.substring(0, 2000);
			sb.append(message).append("\n");
			sb.append("```");
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.botroomchannel, sb.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
