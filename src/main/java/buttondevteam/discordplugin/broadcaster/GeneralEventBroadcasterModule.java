package buttondevteam.discordplugin.broadcaster;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import lombok.Getter;

public class GeneralEventBroadcasterModule extends Component<DiscordPlugin> {
	private static @Getter boolean hooked = false;

	@Override
	protected void enable() {
		try {
			PlayerListWatcher.hookUp();
			DPUtils.getLogger().info("Finished hooking into the player list");
			hooked = true;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while hacking the player list!", e);
		}
	}

	@Override
	protected void disable() {
		try {
			if (PlayerListWatcher.hookDown())
				DPUtils.getLogger().info("Finished unhooking the player list!");
			else
				DPUtils.getLogger().info("Didn't have the player list hooked.");
			hooked = false;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while hacking the player list!", e);
		}
	}
}
