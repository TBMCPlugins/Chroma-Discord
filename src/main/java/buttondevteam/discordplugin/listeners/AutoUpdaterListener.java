package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.DiscordSender;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;

public class AutoUpdaterListener implements IListener<MessageReceivedEvent> {
	@Override
	public void handle(MessageReceivedEvent event) {
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.officechannel.getID()))
			return;
		if (!"239123781401051138".equals(event.getMessage().getWebhookID()))
			return;
		if (event.getMessage().getEmbedded().size() == 0)
			return;
		final String title = event.getMessage().getEmbedded().get(0).getTitle();
		if (!title.contains("new commit"))
			return;
		String branch = title.substring(title.indexOf(':') + 1, title.indexOf(']'));
		String project = title.substring(title.indexOf('[') + 1, title.indexOf(':'));
		if (branch.equals("master") || (TBMCCoreAPI.IsTestServer() && branch.equals("dev")))
			TBMCCoreAPI.UpdatePlugin(project, new DiscordSender(null, DiscordPlugin.officechannel));
	}
}
