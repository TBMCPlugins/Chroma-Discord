package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.lib.TBMCCoreAPI;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.Role;
import lombok.val;

public class CommandListener {
	/**
	 * Runs a ChromaBot command. If mentionedonly is false, it will only execute the command if it was in #bot with the correct prefix or in private.
	 *
	 * @param message       The Discord message
	 * @param mentionedonly Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it ran the command
	 */
	public static boolean runCommand(Message message, boolean mentionedonly) {
		if (!message.getContent().isPresent())
			return false; //Pin messages and such, let the mcchat listener deal with it
		final MessageChannel channel = message.getChannel().block();
		@SuppressWarnings("OptionalGetWithoutIsPresent") val content = message.getContent().get();
		if (channel == null) return false;
		if (!mentionedonly) { //mentionedonly conditions are in CommonListeners
			if (!(channel instanceof PrivateChannel)
				&& !(content.charAt(0) == DiscordPlugin.getPrefix()
				&& channel.getId().asString().equals(DiscordPlugin.plugin.CommandChannel().get().getId().asString()))) //
				return false;
			channel.type().subscribe(); // Fun
		}
		final StringBuilder cmdwithargs = new StringBuilder(content);
		val self=DiscordPlugin.dc.getSelf().block();
		if(self==null) return false;
		val member=self.asMember(DiscordPlugin.mainServer.getId()).block();
		if(member==null) return false;
		final String mention = self.getMention();
		final String mentionNick = member.getNicknameMention();
		boolean gotmention = checkanddeletemention(cmdwithargs, mention, message);
		gotmention = checkanddeletemention(cmdwithargs, mentionNick, message) || gotmention;
		val mentions = message.getRoleMentions();
		for (String mentionRole : member.getRoles().filter(r -> mentions.any(rr -> rr.getName().equals(r.getName())).blockOptional().orElse(false)).map(Role::getMention).toIterable())
			gotmention = checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention; // Delete all mentions
		if (mentionedonly && !gotmention)
			return false;
		channel.type().subscribe();
		String cmdwithargsString = cmdwithargs.toString();
		try {
			if (!DiscordPlugin.plugin.getManager().handleCommand(new Command2DCSender(message), cmdwithargsString))
				DPUtils.reply(message, channel, "Unknown command. Do " + DiscordPlugin.getPrefix() + "help for help.\n" + cmdwithargsString).subscribe();
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to process Discord command: " + cmdwithargsString, e);
		}
		return true;
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, Message message) {
		if (message.getContent().orElse("").startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
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
