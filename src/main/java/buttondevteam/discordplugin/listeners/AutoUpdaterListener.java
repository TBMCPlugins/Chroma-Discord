package buttondevteam.discordplugin.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.PluginUpdater;
import buttondevteam.lib.TBMCCoreAPI;

public class AutoUpdaterListener implements Listener {
	@EventHandler
	public void handle(PluginUpdater.UpdatedEvent event) {
		if (DiscordPlugin.SafeMode)
			return;
		try {
			DPUtils.performNoWait(() -> DiscordPlugin.officechannel.getMessageHistory(10).stream()
					.filter(m -> m.getWebhookLongID() == 239123781401051138L && m.getEmbeds().get(0).getTitle()
							.contains(event.getData().get("repository").getAsJsonObject().get("name").getAsString()))
					.findFirst().get().addReaction(DiscordPlugin.DELIVERED_REACTION));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while reacting to plugin update!", e);
		}
	}
}
