package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.AsyncDiscordEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import sx.blah.discord.api.events.Event;
import sx.blah.discord.handle.impl.events.guild.channel.message.MentionEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;

public class CommandListener implements Listener {
	@SuppressWarnings("unchecked")
	@EventHandler
	public <T extends Event> void onEvent(AsyncDiscordEvent<T> event_) {
		if (event_.getEvent() instanceof MentionEvent)
			onMention((AsyncDiscordEvent<MentionEvent>) event_);
		else if (event_.getEvent() instanceof MessageReceivedEvent)
			onMessageReceived((AsyncDiscordEvent<MessageReceivedEvent>) event_);
	}

	private void onMention(AsyncDiscordEvent<MentionEvent> event) {
		//TODO: Can't use priorities with this
	}

	private void onMessageReceived(AsyncDiscordEvent<MessageReceivedEvent> event) {

	}
}
