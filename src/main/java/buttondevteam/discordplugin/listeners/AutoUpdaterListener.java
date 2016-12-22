package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

public class AutoUpdaterListener implements IListener<MessageReceivedEvent> {
	@Override
	public void handle(MessageReceivedEvent event) {
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.officechannel))
			return;
		if (event.getMessage().getWebhookID() == null)
			return;
		System.out.println(event.getMessage().getWebhookID());
		if (event.getMessage().getEmbedded().size() == 0) {
			System.out.println("No embed");
			return;
		}
		System.out.println(event.getMessage().getEmbedded().get(0).getDescription());
	}
}
