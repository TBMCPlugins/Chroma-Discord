package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

public class ChatListener implements IListener<MessageReceivedEvent> {

	@Override
	public void handle(MessageReceivedEvent event) {
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.chatchannel.getID()))
			return;
		if (event.getMessage().getContent().startsWith("/"))
			; // Call API method
		else
			;
	}

}
