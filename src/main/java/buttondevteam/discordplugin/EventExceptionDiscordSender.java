package buttondevteam.discordplugin;

import org.bukkit.event.Event;

public class EventExceptionDiscordSender implements EventExceptionHandler {
	@Override
	public boolean handle(Throwable ex, Event event) {
		TBMCDiscordAPI.SendException(ex, "An error occured while executing " + event.getEventName() + "!");
		return true;
	}
}
