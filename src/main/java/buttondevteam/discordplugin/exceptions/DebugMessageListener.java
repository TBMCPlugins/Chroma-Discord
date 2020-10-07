package buttondevteam.discordplugin.exceptions;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCDebugMessageEvent;
import discord4j.core.object.entity.channel.MessageChannel;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import reactor.core.publisher.Mono;

public class DebugMessageListener implements Listener {
	@EventHandler
	public void onDebugMessage(TBMCDebugMessageEvent e) {
		SendMessage(e.getDebugMessage());
		e.setSent();
	}

	private static void SendMessage(String message) {
		if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(ExceptionListenerModule.class))
			return;
		try {
			Mono<MessageChannel> mc = ExceptionListenerModule.getChannel();
			if (mc == null) return;
			StringBuilder sb = new StringBuilder();
			sb.append("```").append("\n");
			if (message.length() > 2000)
				message = message.substring(0, 2000);
			sb.append(message).append("\n");
			sb.append("```");
			mc.flatMap(ch -> ch.createMessage(sb.toString())).subscribe();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}
