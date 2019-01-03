package buttondevteam.discordplugin.broadcaster;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import lombok.Getter;
import org.bukkit.Bukkit;

public class GeneralEventBroadcasterModule extends Component {
	private static @Getter boolean hooked = false;

	@Override
	protected void enable() {
		try {
			PlayerListWatcher.hookUp();
			Bukkit.getLogger().info("Finished hooking into the player list");
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
