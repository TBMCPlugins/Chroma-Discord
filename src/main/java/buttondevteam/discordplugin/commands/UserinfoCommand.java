package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.ChromaGamerBase.InfoTarget;
import lombok.val;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Message;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CommandClass(helpText = {
	"User information", //
	"Shows some information about users, from Discord, from Minecraft or from Reddit if they have these accounts connected.", //
	"If used without args, shows your info.", //
})
public class UserinfoCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean def(Command2DCSender sender, @Command2.OptionalArg @Command2.TextArg String user) {
		val message = sender.getMessage();
		IUser target = null;
		if (user == null || user.length() == 0)
			target = message.getAuthor();
		else {
			final Optional<IUser> firstmention = message.getMentions().stream()
					.filter(m -> !m.getStringID().equals(DiscordPlugin.dc.getOurUser().getStringID())).findFirst();
			if (firstmention.isPresent())
				target = firstmention.get();
			else if (user.contains("#")) {
				String[] targettag = user.split("#");
				final List<IUser> targets = getUsers(message, targettag[0]);
				if (targets.size() == 0) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"The user cannot be found (by name): " + user);
                    return true;
				}
				for (IUser ptarget : targets) {
					if (ptarget.getDiscriminator().equalsIgnoreCase(targettag[1])) {
						target = ptarget;
						break;
					}
				}
				if (target == null) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"The user cannot be found (by discriminator): " + user + "(Found " + targets.size()
									+ " users with the name.)");
                    return true;
				}
			} else {
				final List<IUser> targets = getUsers(message, user);
				if (targets.size() == 0) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"The user cannot be found on Discord: " + user);
                    return true;
				}
				if (targets.size() > 1) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
							"Multiple users found with that (nick)name. Please specify the whole tag, like ChromaBot#6338 or use a ping.");
                    return true;
				}
				target = targets.get(0);
			}
		}
		try (DiscordPlayer dp = ChromaGamerBase.getUser(target.getStringID(), DiscordPlayer.class)) {
			StringBuilder uinfo = new StringBuilder("User info for ").append(target.getName()).append(":\n");
			uinfo.append(dp.getInfo(InfoTarget.Discord));
			DiscordPlugin.sendMessageToChannel(message.getChannel(), uinfo.toString());
		} catch (Exception e) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "An error occured while getting the user!");
			TBMCCoreAPI.SendException("Error while getting info about " + target.getName() + "!", e);
		}
        return true;
	}

	private List<IUser> getUsers(Message message, String args) {
		final List<IUser> targets;
		if (message.getChannel().isPrivate())
			targets = DiscordPlugin.dc.getUsers().stream().filter(u -> u.getName().equalsIgnoreCase(args))
					.collect(Collectors.toList());
		else
			targets = message.getGuild().getUsersByName(args, true);
		return targets;
	}

}
