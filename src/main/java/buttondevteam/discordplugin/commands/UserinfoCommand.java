package buttondevteam.discordplugin.commands;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayer.InfoTarget;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IUser;

public class UserinfoCommand extends DiscordCommandBase {

	@Override
	public String getCommandName() {
		return "userinfo";
	}

	@Override
	public void run(IMessage message, String args) {
		if (args.contains(" ")) {
			DiscordPlugin.sendMessageToChannel(message.getChannel(),
					"Too many arguments.\nUsage: userinfo [username/nickname[#tag]/ping]\nExamples:\nuserinfo ChromaBot\nuserinfo ChromaBot#6338\nuserinfo @ChromaBot#6338");
			return;
		}
		IUser target = null;
		if (args.length() == 0)
			target = message.getAuthor();
		else {
			final Stream<IUser> mentions = message.getMentions().stream()
					.filter(m -> !m.getID().equals(DiscordPlugin.dc.getOurUser().getID()));
			if (mentions.findFirst().isPresent())
				target = mentions.findFirst().get();
			else if (args.contains("#")) {
				String[] targettag = args.split("#");
				final List<IUser> targets = getUsers(message, targettag[0]);
				if (targets.size() == 0) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
							"The user cannot be found (by name): " + args);
					return;
				}
				for (IUser ptarget : targets) {
					if (ptarget.getDiscriminator().equalsIgnoreCase(targettag[1])) {
						target = ptarget;
						break;
					}
				}
				if (target == null) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
							"The user cannot be found (by discriminator): " + args + "(Found " + targets.size()
									+ " users with the name.)");
					return;
				}
			} else {
				final List<IUser> targets = getUsers(message, args);
				if (targets.size() == 0) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
							"The user cannot be found on Discord: " + args);
					return;
				}
				if (targets.size() > 1) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
							"Multiple users found with that (nick)name. Please specify the whole tag, like ChromaBot#6338 or use a ping.");
					return;
				}
				target = targets.get(0);
			}
		}
		boolean found = false;
		for (TBMCPlayer player : TBMCPlayer.getLoadedPlayers().values()) {
			DiscordPlayer dp = player.asPluginPlayer(DiscordPlayer.class);
			if (target.getID().equals(dp.getDiscordID())) {
				StringBuilder uinfo = new StringBuilder("User info for ").append(target.getName()).append(":\n");
				uinfo.append(player.getInfo(InfoTarget.Discord));
				DiscordPlugin.sendMessageToChannel(message.getChannel(), uinfo.toString());
				found = true;
				break;
			}
		}
		if (!found)
			DiscordPlugin.sendMessageToChannel(message.getChannel(), "The user (" + target.getName()
					+ ") is not found in our system (player has to be on the MC server for now)!");
	}

	private List<IUser> getUsers(IMessage message, String args) {
		final List<IUser> targets;
		if (message.getChannel().isPrivate())
			targets = DiscordPlugin.dc.getUsers().stream().filter(u -> u.getName().equalsIgnoreCase(args))
					.collect(Collectors.toList());
		else
			targets = message.getGuild().getUsersByName(args, true);
		return targets;
	}

	@Override
	public String[] getHelpText() {
		return new String[] { //
				"---- User information ----", //
				"Shows some information about users, from Discord, from Minecraft or from Reddit if they have these accounts connected.", //
				"Usage: userinfo <Discordname>" //
		};
	}

}
