package buttondevteam.discordplugin;

import org.bukkit.event.Event;
import buttondevteam.lib.EventExceptionHandler;

public class EventExceptionDiscordSender extends EventExceptionHandler {
	@Override
	public boolean handle(Throwable ex, Event event) {
		TBMCDiscordAPI.SendException(ex, "An error occured while executing " + event.getEventName() + "!");
		return true;
	}
}
