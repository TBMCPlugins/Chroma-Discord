package buttondevteam.discordplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCChatEvent;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
	@EventHandler
	public void onMCChat(TBMCChatEvent e) {
		DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
				"[" + e.getChannel().DisplayName + "] <" + e.getSender().getName() + "> " + e.getMessage());
	}

	@Override
	public void handle(MessageReceivedEvent event) {
		if (event.getMessage().getAuthor().isBot())
			return;
		if (event.getMessage().getChannel().getID().equals(DiscordPlugin.chatchannel.getID()))
			TBMCChatAPI.SendChatMessage(Channel.GlobalChat, Bukkit.getConsoleSender(), event.getMessage().getContent());
	}
}
