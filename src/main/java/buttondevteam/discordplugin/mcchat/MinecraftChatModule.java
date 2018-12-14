package buttondevteam.discordplugin.mcchat;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;

public class MinecraftChatModule extends Component {
	@Override
	protected void enable() {
		MCChatListener mcchat = new MCChatListener();
		DiscordPlugin.dc.getDispatcher().registerListener(mcchat);
		TBMCCoreAPI.RegisterEventsForExceptions(mcchat, getPlugin());
	}

	@Override
	protected void disable() {
		//These get undone if restarting/resetting - it will ignore events if disabled
	} //TODO: Use ComponentManager.isEnabled() at other places too, instead of SafeMode
}
