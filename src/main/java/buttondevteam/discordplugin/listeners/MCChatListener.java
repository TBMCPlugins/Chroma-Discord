package buttondevteam.discordplugin.listeners;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.DiscordSender;
import buttondevteam.lib.TBMCChatEvent;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
	@EventHandler // Minecraft
	public void onMCChat(TBMCChatEvent e) {
		if (e.getSender() instanceof DiscordSender)
			return;
		if (e.getChannel().equals(Channel.GlobalChat))
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
					"<" + e.getSender().getName() + "> " + e.getMessage());
	}

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		if (event.getMessage().getAuthor().isBot())
			return;
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.chatchannel.getID())
		/* && !(event.getMessage().getChannel().isPrivate() && privatechat) */)
			return;
		final DiscordSender dsender = new DiscordSender(event.getMessage().getAuthor());
		dsender.setChannel(event.getMessage().getChannel());
		if (event.getMessage().getContent().startsWith("/")) {
			final String cmd = event.getMessage().getContent().substring(1);
			try {
				Bukkit.dispatchCommand(dsender, cmd);
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while executing command " + cmd + "!", e);
				return;
			}
		} else
			TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender, event.getMessage().getContent());
	}
}
