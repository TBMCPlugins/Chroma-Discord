package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.discordplugin.util.Timings;
import buttondevteam.lib.TBMCCoreAPI;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.PrivateChannel;
import lombok.val;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicBoolean;

public class CommandListener {
	/**
	 * Runs a ChromaBot command. If mentionedonly is false, it will only execute the command if it was in #bot with the correct prefix or in private.
	 *
	 * @param message       The Discord message
	 * @param mentionedonly Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it <b>did not run</b> the command
	 */
	public static Mono<Boolean> runCommand(Message message, Snowflake commandChannelID, boolean mentionedonly) {
		Timings timings = CommonListeners.timings;
		Mono<Boolean> ret = Mono.just(true);
		if (message.getContent().length() == 0)
			return ret; //Pin messages and such, let the mcchat listener deal with it
		val content = message.getContent();
		timings.printElapsed("A");
		return message.getChannel().flatMap(channel -> {
			Mono<?> tmp = ret;
			if (!mentionedonly) { //mentionedonly conditions are in CommonListeners
				timings.printElapsed("B");
				if (!(channel instanceof PrivateChannel)
					&& !(content.charAt(0) == DiscordPlugin.getPrefix()
					&& channel.getId().asLong() == commandChannelID.asLong())) //
					return ret;
				timings.printElapsed("C");
				tmp = ret.then(channel.type()).thenReturn(true); // Fun (this true is ignored - x)
			}
			final StringBuilder cmdwithargs = new StringBuilder(content);
			val gotmention = new AtomicBoolean();
			timings.printElapsed("Before self");
			return tmp.flatMapMany(x ->
				DiscordPlugin.dc.getSelf().flatMap(self -> self.asMember(DiscordPlugin.mainServer.getId()))
					.flatMapMany(self -> {
						timings.printElapsed("D");
						gotmention.set(checkanddeletemention(cmdwithargs, self.getMention(), message));
						gotmention.set(checkanddeletemention(cmdwithargs, self.getNicknameMention(), message) || gotmention.get());
						val mentions = message.getRoleMentions();
						return self.getRoles().filterWhen(r -> mentions.any(rr -> rr.getName().equals(r.getName())))
							.map(Role::getMention);
					}).map(mentionRole -> {
					timings.printElapsed("E");
					gotmention.set(checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention.get()); // Delete all mentions
					return !mentionedonly || gotmention.get(); //Stops here if false
				}).switchIfEmpty(Mono.fromSupplier(() -> !mentionedonly || gotmention.get())))
				.filter(b -> b).last(false).filter(b -> b).doOnNext(b -> channel.type().subscribe()).flatMap(b -> {
					String cmdwithargsString = cmdwithargs.toString();
					try {
						timings.printElapsed("F");
						if (!DiscordPlugin.plugin.getManager().handleCommand(new Command2DCSender(message), cmdwithargsString))
							return DPUtils.reply(message, channel, "unknown command. Do " + DiscordPlugin.getPrefix() + "help for help.")
								.map(m -> false);
					} catch (Exception e) {
						TBMCCoreAPI.SendException("Failed to process Discord command: " + cmdwithargsString, e, DiscordPlugin.plugin);
					}
					return Mono.just(false); //If the command succeeded or there was an error, return false
				}).defaultIfEmpty(true);
		});
	}

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, Message message) {
		final char prefix = DiscordPlugin.getPrefix();
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
				cmdwithargs.insert(0, prefix); //Always use the prefix for processing
			} else
				cmdwithargs.replace(0, cmdwithargs.length(), prefix + "help");
		else {
			if (cmdwithargs.length() == 0)
				cmdwithargs.replace(0, 0, prefix + "help");
			else if (cmdwithargs.charAt(0) != prefix)
				cmdwithargs.insert(0, prefix);
			return false; //Don't treat / as mention, mentions can be used in public mcchat
		}
		return true;
	}
}
