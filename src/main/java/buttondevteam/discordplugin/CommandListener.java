package buttondevteam.discordplugin;

import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MentionEvent;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class CommandListener {

	public static IListener<?>[] getListeners() {
		return new IListener[] { new IListener<MentionEvent>() {
			@Override
			public void handle(MentionEvent event) {
				final IChannel channel = event.getMessage().getChannel();
				if (!channel.getID().equals(DiscordPlugin.botchannel.getID()) && !channel.isPrivate())
					return;
				runCommand(event.getMessage());
			}
		}, new IListener<MessageReceivedEvent>() {
			@Override
			public void handle(MessageReceivedEvent event) {
				if (!event.getMessage().getChannel().isPrivate())
					return;
				runCommand(event.getMessage());
			}
		} };
	}

	private static void runCommand(IMessage message) {
		System.out.println(message.getContent()); // TODO: Resolve mentions
	}
}
