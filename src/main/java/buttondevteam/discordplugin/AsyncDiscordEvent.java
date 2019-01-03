package buttondevteam.discordplugin;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@RequiredArgsConstructor
public class AsyncDiscordEvent<T extends sx.blah.discord.api.events.Event> extends Event implements Cancellable {
	private final @Getter T event;
	@Getter
	@Setter
	private boolean cancelled;

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
