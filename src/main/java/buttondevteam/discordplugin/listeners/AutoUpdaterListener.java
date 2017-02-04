package buttondevteam.discordplugin.listeners;

import java.awt.Color;
import java.util.function.Supplier;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.DiscordSender;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IEmbed;
import sx.blah.discord.util.EmbedBuilder;

public class AutoUpdaterListener implements IListener<MessageReceivedEvent> {
	@Override
	public void handle(MessageReceivedEvent event) {
		if (DiscordPlugin.SafeMode)
			return;
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.officechannel.getID()))
			return;
		if (!"239123781401051138".equals(event.getMessage().getWebhookID()))
			return;
		if (event.getMessage().getEmbedded().size() == 0)
			return;
		final IEmbed embed = event.getMessage().getEmbedded().get(0);
		final String title = embed.getTitle();
		if (!title.contains("new commit"))
			return;
		String branch = title.substring(title.indexOf(':') + 1, title.indexOf(']'));
		String project = title.substring(title.indexOf('[') + 1, title.indexOf(':'));
		if ((branch.equals("master") || (TBMCCoreAPI.IsTestServer() && branch.equals("dev")))
				&& TBMCCoreAPI.UpdatePlugin(project,
						new DiscordSender(null,
								TBMCCoreAPI.IsTestServer() //
										? DiscordPlugin.chatchannel //
										: DiscordPlugin.botroomchannel),
						branch)
				&& ((Supplier<Boolean>) () -> { // Best looking code I've ever written
					try {
						int hi, ei, prnum;
						if ((hi = embed.getDescription().indexOf('#')) > -1
								&& ((ei = embed.getDescription().indexOf(' ', hi + 1)) > -1
										|| (ei = embed.getDescription().indexOf(".", hi + 1)) > -1
										|| (ei = embed.getDescription().length()) > -1)
								&& (prnum = Integer.parseInt(embed.getDescription().substring(hi + 1, ei))) > -1)
							DiscordPlugin.sendMessageToChannel(DiscordPlugin.updatechannel, "",
									new EmbedBuilder().withColor(Color.WHITE).withTitle("Update details")
											.withUrl("https://github.com/TBMCPlugins/" + project + "/pull/" + prnum)
											.build());
						else
							throw new Exception("No PR found");
					} catch (Exception e) {
						DiscordPlugin.sendMessageToChannel(DiscordPlugin.updatechannel, "",
								new EmbedBuilder().withColor(Color.WHITE).withTitle("Update details:")
										.withDescription(embed.getDescription() + " (" + e.getMessage() + ")").build());
					}
					return true;
				}).get() && (!TBMCCoreAPI.IsTestServer() || !branch.equals("master")))
			try {
				DiscordPlugin.perform(() -> event.getMessage().addReaction(DiscordPlugin.DELIVERED_REACTION));
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while reacting to plugin update!", e);
			}
	}
}
