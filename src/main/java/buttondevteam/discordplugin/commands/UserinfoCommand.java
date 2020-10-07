package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.ChromaGamerBase.InfoTarget;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.User;
import lombok.val;

import java.util.List;

@CommandClass(helpText = {
	"User information", //
	"Shows some information about users, from Discord, from Minecraft or from Reddit if they have these accounts connected.", //
	"If used without args, shows your info.", //
})
public class UserinfoCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean def(Command2DCSender sender, @Command2.OptionalArg @Command2.TextArg String user) {
		val message = sender.getMessage();
		User target = null;
		val channel = message.getChannel().block();
		assert channel != null;
		if (user == null || user.length() == 0)
			target = message.getAuthor().orElse(null);
		else {
			final User firstmention = message.getUserMentions()
				.filter(m -> !m.getId().asString().equals(DiscordPlugin.dc.getSelfId().asString())).blockFirst();
			if (firstmention != null)
				target = firstmention;
			else if (user.contains("#")) {
				String[] targettag = user.split("#");
				final List<User> targets = getUsers(message, targettag[0]);
				if (targets.size() == 0) {
					channel.createMessage("The user cannot be found (by name): " + user).subscribe();
					return true;
				}
				for (User ptarget : targets) {
					if (ptarget.getDiscriminator().equalsIgnoreCase(targettag[1])) {
						target = ptarget;
						break;
					}
				}
				if (target == null) {
					channel.createMessage("The user cannot be found (by discriminator): " + user + "(Found " + targets.size()
						+ " users with the name.)").subscribe();
					return true;
				}
			} else {
				final List<User> targets = getUsers(message, user);
				if (targets.size() == 0) {
					channel.createMessage("The user cannot be found on Discord: " + user).subscribe();
					return true;
				}
				if (targets.size() > 1) {
					channel.createMessage("Multiple users found with that (nick)name. Please specify the whole tag, like ChromaBot#6338 or use a ping.").subscribe();
					return true;
				}
				target = targets.get(0);
			}
		}
		if (target == null) {
			sender.sendMessage("An error occurred.");
			return true;
		}
		try (DiscordPlayer dp = ChromaGamerBase.getUser(target.getId().asString(), DiscordPlayer.class)) {
			StringBuilder uinfo = new StringBuilder("User info for ").append(target.getUsername()).append(":\n");
			uinfo.append(dp.getInfo(InfoTarget.Discord));
			channel.createMessage(uinfo.toString()).subscribe();
		} catch (Exception e) {
			channel.createMessage("An error occured while getting the user!").subscribe();
			TBMCCoreAPI.SendException("Error while getting info about " + target.getUsername() + "!", e);
		}
		return true;
	}

	private List<User> getUsers(Message message, String args) {
		final List<User> targets;
		val guild = message.getGuild().block();
		if (guild == null) //Private channel
			targets = DiscordPlugin.dc.getUsers().filter(u -> u.getUsername().equalsIgnoreCase(args))
				.collectList().block();
		else
			targets = guild.getMembers().filter(m -> m.getUsername().equalsIgnoreCase(args))
				.map(m -> (User) m).collectList().block();
		return targets;
	}

}
