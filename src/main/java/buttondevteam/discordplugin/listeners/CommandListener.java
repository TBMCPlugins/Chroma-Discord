package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;

public class CommandListener {
	/**
	 * Runs a ChromaBot command. If mentionedonly is false, it will only execute the command if it was in #bot with the correct prefix or in private.
	 *
	 * @param message       The Discord message
	 * @param mentionedonly Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it ran the command
	 */
	public static boolean runCommand(IMessage message, boolean mentionedonly) {
		final IChannel channel = message.getChannel();
		if (mentionedonly) {
			if (!channel.getStringID().equals(DiscordPlugin.botchannel.getStringID())
					&& !message.getContent().contains("channelcon")) //Allow channelcon in other servers
				return false; //Private chat is handled without mentions
		} else {
			if (!message.getChannel().isPrivate()
					&& !(message.getContent().startsWith("/")
					&& channel.getStringID().equals(DiscordPlugin.botchannel.getStringID()))) //
				return false;
		}
		message.getChannel().setTypingStatus(true); // Fun
		final StringBuilder cmdwithargs = new StringBuilder(message.getContent());
		final String mention = DiscordPlugin.dc.getOurUser().mention(false);
		final String mentionNick = DiscordPlugin.dc.getOurUser().mention(true);
		boolean gotmention = checkanddeletemention(cmdwithargs, mention, message);
		gotmention = checkanddeletemention(cmdwithargs, mentionNick, message) || gotmention;
		for (String mentionRole : (Iterable<String>) message.getRoleMentions().stream().filter(r -> DiscordPlugin.dc.getOurUser().hasRole(r)).map(r -> r.mention())::iterator)
			gotmention = checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention; // Delete all mentions
		if (mentionedonly && !gotmention) {
			message.getChannel().setTypingStatus(false);
			return false;
		}
		message.getChannel().setTypingStatus(true);
		String cmdwithargsString = cmdwithargs.toString().trim(); //Remove spaces between mention and command
		int index = cmdwithargsString.indexOf(" ");
		String cmd;
		String args;
		if (index == -1) {
			cmd = cmdwithargsString;
			args = "";
		} else {
			cmd = cmdwithargsString.substring(0, index);
			args = cmdwithargsString.substring(index + 1).trim(); //In case there are multiple spaces
		}
		DiscordCommandBase.runCommand(cmd.toLowerCase(), args, message);
		message.getChannel().setTypingStatus(false);
		return true;
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, IMessage message) {
		if (message.getContent().startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
			if (cmdwithargs.length() > mention.length() + 1)
				cmdwithargs.delete(0,
						cmdwithargs.charAt(mention.length()) == ' ' ? mention.length() + 1 : mention.length());
			else
				cmdwithargs.replace(0, cmdwithargs.length(), "help");
		else {
			if (cmdwithargs.length() > 0 && cmdwithargs.charAt(0) == '/')
				cmdwithargs.deleteCharAt(0); //Don't treat / as mention, mentions can be used in public mcchat
			return false;
		}
		if (cmdwithargs.length() == 0)
			cmdwithargs.replace(0, cmdwithargs.length(), "help");
		return true;
	}
}
