package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import lombok.Getter;

public class MinecraftChatModule extends Component {
	private @Getter MCChatListener listener;

	public MCChatListener getListener() { //It doesn't want to generate
		return listener;
	}
	@Override
	protected void enable() {
		listener = new MCChatListener();
		DiscordPlugin.dc.getDispatcher().registerListener(listener);
		TBMCCoreAPI.RegisterEventsForExceptions(listener, getPlugin());
		TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), getPlugin());
	}

	@Override
	protected void disable() {
		//These get undone if restarting/resetting - it will ignore events if disabled
	} //TODO: Use ComponentManager.isEnabled() at other places too, instead of SafeMode
}
