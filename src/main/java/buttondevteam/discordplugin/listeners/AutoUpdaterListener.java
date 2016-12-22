package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

public class AutoUpdaterListener implements IListener<MessageReceivedEvent> {
	@Override
	public void handle(MessageReceivedEvent event) {
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.officechannel.getID()))
			return;
		if (event.getMessage().getWebhookID() == null)
			return;
		System.out.println(event.getMessage().getWebhookID());
		if (event.getMessage().getEmbedded().size() == 0) {
			System.out.println("No embed");
			return;
		}
		final String title = event.getMessage().getEmbedded().get(0).getTitle();
		System.out.println(title);
		System.out.println(title.indexOf(':'));
		System.out.println(title.indexOf(']'));
		System.out.println(title.substring(title.indexOf(':') + 1, title.indexOf(']')));
		System.out.println(title.contains("new commit"));
	}
}
