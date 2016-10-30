package buttondevteam.discordplugin.commands;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCPlayer;
import sx.blah.discord.handle.impl.obj.Guild;
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
		else if (message.getMentions().size() > 0)
			target = message.getMentions().get(0);
		else if (args.contains("#")) {
			String[] targettag = args.split("#");
			final List<IUser> targets = message.getGuild().getUsersByName(targettag[0], true);
			if (targets.size() == 0) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "The user cannot be found (by name): " + args);
				return;
			}
			for (IUser ptarget : targets) {
				if (ptarget.getDiscriminator().equalsIgnoreCase(targettag[1])) {
					target = ptarget;
					break;
				}
				if (target == null) {
					DiscordPlugin.sendMessageToChannel(message.getChannel(),
							"The user cannot be found (by discriminator): " + args + "(Found " + targets.size()
									+ " users with the name.)");
					return;
				}
			}
		} else {
			final List<IUser> targets = message.getGuild().getUsersByName(args, true);
			if (targets.size() == 0) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "The user cannot be found: " + args);
				return;
			}
			if (targets.size() > 1) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(),
						"Multiple users found with that (nick)name. Please specify the whole tag, like ChromaBot#6338 or use a ping.");
				return;
			}
			target = targets.get(0);
		}
		for (Player p : Bukkit.getOnlinePlayers()) {
			DiscordPlayer dp = TBMCPlayer.getPlayer(p).asPluginPlayer(DiscordPlayer.class);
			if (target.getID().equals(dp.getDiscordID())) {
				DiscordPlugin.sendMessageToChannel(message.getChannel(), "User info for " + target.getName());
				break; // TODO: Get user data
			}
		}
	}

}
