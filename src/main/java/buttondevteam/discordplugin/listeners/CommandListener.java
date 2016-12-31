package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
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
				if (DiscordPlugin.SafeMode)
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				final IChannel channel = event.getMessage().getChannel();
				if (!channel.getID().equals(DiscordPlugin.botchannel.getID()) && !channel.isPrivate())
					return;
				runCommand(event.getMessage(), true);
			}
		}, new IListener<MessageReceivedEvent>() {
			@Override
			public void handle(MessageReceivedEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				if (!event.getMessage().getChannel().isPrivate())
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				runCommand(event.getMessage(), false);
			}
		} };
	}

	/**
	 * Runs a ChromaBot command.
	 * 
	 * @param message
	 *            The Discord message
	 * @param mentionedonly
	 *            Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it ran the command (always true if mentionedonly is false)
	 */
	public static boolean runCommand(IMessage message, boolean mentionedonly) {
		if (DiscordPlugin.SafeMode)
			return true;
		message.getChannel().setTypingStatus(true);
		final StringBuilder cmdwithargs = new StringBuilder(message.getContent());
		final String mention = DiscordPlugin.dc.getOurUser().mention(false);
		final String mentionNick = DiscordPlugin.dc.getOurUser().mention(true);
		boolean gotmention = checkanddeletemention(cmdwithargs, mention, message);
		gotmention = checkanddeletemention(cmdwithargs, mentionNick, message);
		for (String mentionRole : (Iterable<String>) message.getRoleMentions().stream().map(r -> r.mention())::iterator)
			gotmention = checkanddeletemention(cmdwithargs, mentionRole, message);
		if (mentionedonly && !gotmention) {
			message.getChannel().setTypingStatus(false);
			return false;
		}
		int index = cmdwithargs.indexOf(" ");
		String cmd;
		String args;
		if (index == -1) {
			cmd = cmdwithargs.toString();
			args = "";
		} else {
			cmd = cmdwithargs.substring(0, index);
			args = cmdwithargs.substring(index + 1);
		}
		DiscordCommandBase.runCommand(cmd, args, message);
		message.getChannel().setTypingStatus(false);
		return true;
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, IMessage message) {
		if (message.getContent().startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
			if (cmdwithargs.length() > mention.length() + 1)
				cmdwithargs = cmdwithargs.delete(0,
						cmdwithargs.charAt(mention.length()) == ' ' ? mention.length() + 1 : mention.length());
			else
				cmdwithargs.replace(0, cmdwithargs.length(), "help");
		else
			return false;
		if (cmdwithargs.length() == 0)
			cmdwithargs.replace(0, cmdwithargs.length(), "help");
		return true;
	}
}
