package buttondevteam.discordplugin;

import buttondevteam.discordplugin.commands.DiscordCommandBase;
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
				if (event.getMessage().getAuthor().isBot())
					return;
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
				if (event.getMessage().getAuthor().isBot())
					return;
				runCommand(event.getMessage());
			}
		} };
	}

	private static void runCommand(IMessage message) {
		String cmdwithargs = message.getContent();
		final String mention = DiscordPlugin.dc.getOurUser().mention(false);
		final String mentionNick = DiscordPlugin.dc.getOurUser().mention(true);
		if (message.getContent().startsWith(mention) && cmdwithargs.length() > mention.length() + 1) // TODO: Resolve mentions: Compound arguments, either a mention or text
			cmdwithargs = cmdwithargs.substring(mention.length() + 1);
		if (message.getContent().startsWith(mentionNick) && cmdwithargs.length() > mentionNick.length() + 1)
			cmdwithargs = cmdwithargs.substring(mentionNick.length() + 1);
		int index = cmdwithargs.indexOf(' ');
		String cmd;
		String args;
		if (index == -1) {
			cmd = cmdwithargs;
			args = "";
		} else {
			cmd = cmdwithargs.substring(0, index);
			args = cmdwithargs.substring(index + 1);
		}
		DiscordCommandBase.runCommand(cmd, args, message);
	}
}
