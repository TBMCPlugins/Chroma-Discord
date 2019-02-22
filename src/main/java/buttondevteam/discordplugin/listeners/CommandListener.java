package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;

public class CommandListener {
	/**
	 * Runs a ChromaBot command. If mentionedonly is false, it will only execute the command if it was in #bot with the correct prefix or in private.
	 *
	 * @param message       The Discord message
	 * @param mentionedonly Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it ran the command
	 */
	public static boolean runCommand(IMessage message, boolean mentionedonly) {
		if (message.getContent().length() == 0)
			return false; //Pin messages and such, let the mcchat listener deal with it
		final IChannel channel = message.getChannel();
		if (!mentionedonly) { //mentionedonly conditions are in CommonListeners
			if (!message.getChannel().isPrivate()
				&& !(message.getContent().charAt(0) == DiscordPlugin.getPrefix()
				&& channel.getStringID().equals(DiscordPlugin.plugin.CommandChannel().get().getStringID()))) //
				return false;
			message.getChannel().setTypingStatus(true); // Fun
		}
		final StringBuilder cmdwithargs = new StringBuilder(message.getContent());
		final String mention = DiscordPlugin.dc.getOurUser().mention(false);
		final String mentionNick = DiscordPlugin.dc.getOurUser().mention(true);
		boolean gotmention = checkanddeletemention(cmdwithargs, mention, message);
		gotmention = checkanddeletemention(cmdwithargs, mentionNick, message) || gotmention;
		for (String mentionRole : (Iterable<String>) message.getRoleMentions().stream().filter(r -> DiscordPlugin.dc.getOurUser().hasRole(r)).map(IRole::mention)::iterator)
			gotmention = checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention; // Delete all mentions
		if (mentionedonly && !gotmention) {
			message.getChannel().setTypingStatus(false);
			return false;
		}
		message.getChannel().setTypingStatus(true);
		String cmdwithargsString = cmdwithargs.toString();
		try {
			if (!DiscordPlugin.plugin.getManager().handleCommand(new Command2DCSender(message), cmdwithargsString))
				message.reply("Unknown command. Do " + DiscordPlugin.getPrefix() + "help for help.\n" + cmdwithargsString);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to process Discord command: " + cmdwithargsString, e);
		}
		message.getChannel().setTypingStatus(false);
		return true;
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, IMessage message) {
		if (message.getContent().startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
			if (cmdwithargs.length() > mention.length() + 1) {
				int i = cmdwithargs.indexOf(" ", mention.length());
				if (i == -1)
					i = mention.length();
				else
					//noinspection StatementWithEmptyBody
					for (; i < cmdwithargs.length() && cmdwithargs.charAt(i) == ' '; i++)
						; //Removes any space before the command
				cmdwithargs.delete(0, i);
				cmdwithargs.insert(0, DiscordPlugin.getPrefix()); //Always use the prefix for processing
			} else
				cmdwithargs.replace(0, cmdwithargs.length(), DiscordPlugin.getPrefix() + "help");
		else {
			return false; //Don't treat / as mention, mentions can be used in public mcchat
		}
		if (cmdwithargs.length() == 0)
			cmdwithargs.replace(0, cmdwithargs.length(), DiscordPlugin.getPrefix() + "help");
		return true;
	}
}
