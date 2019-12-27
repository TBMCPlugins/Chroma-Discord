package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.discordplugin.commands.VersionCommand;
import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.ICommand2MC;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;

@CommandClass(path = "discord", helpText = {
	"Discord",
	"This command allows performing Discord-related actions."
})
public class DiscordMCCommand extends ICommand2MC {
	@Command2.Subcommand
	public boolean accept(Player player) {
		String did = ConnectCommand.WaitingToConnect.get(player.getName());
		if (did == null) {
			player.sendMessage("§cYou don't have a pending connection to Discord.");
			return true;
		}
		DiscordPlayer dp = ChromaGamerBase.getUser(did, DiscordPlayer.class);
		TBMCPlayer mcp = TBMCPlayerBase.getPlayer(player.getUniqueId(), TBMCPlayer.class);
		dp.connectWith(mcp);
		dp.save();
		mcp.save();
		ConnectCommand.WaitingToConnect.remove(player.getName());
		MCChatUtils.UnconnectedSenders.remove(did); //Remove all unconnected, will be recreated where needed
		player.sendMessage("§bAccounts connected.");
		return true;
	}

	@Command2.Subcommand
	public boolean decline(Player player) {
		String did = ConnectCommand.WaitingToConnect.remove(player.getName());
		if (did == null) {
			player.sendMessage("§cYou don't have a pending connection to Discord.");
			return true;
		}
		player.sendMessage("§bPending connection declined.");
		return true;
	}

	@Command2.Subcommand(permGroup = Command2.Subcommand.MOD_GROUP, helpText = {
		"Reload Discord plugin",
		"Reloads the config. To apply some changes, you may need to also run /discord reset."
	})
	public void reload(CommandSender sender) {
		if (DiscordPlugin.plugin.tryReloadConfig())
			sender.sendMessage("§bConfig reloaded.");
		else
			sender.sendMessage("§cFailed to reload config.");
	}

	public static boolean resetting = false;

	@Command2.Subcommand(permGroup = Command2.Subcommand.MOD_GROUP, helpText = {
		"Reset ChromaBot", //
		"This command disables and then enables the plugin." //
	})
	public void reset(CommandSender sender) {
		Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
			if (!DiscordPlugin.plugin.tryReloadConfig()) {
				sender.sendMessage("§cFailed to reload config so not resetting. Check the console.");
				return;
			}
			resetting = true; //Turned off after sending enable message (ReadyEvent)
			sender.sendMessage("§bDisabling DiscordPlugin...");
			Bukkit.getPluginManager().disablePlugin(DiscordPlugin.plugin);
			if (!(sender instanceof DiscordSenderBase)) //Sending to Discord errors
				sender.sendMessage("§bEnabling DiscordPlugin...");
			Bukkit.getPluginManager().enablePlugin(DiscordPlugin.plugin);
			if (!(sender instanceof DiscordSenderBase)) //Sending to Discord errors
				sender.sendMessage("§bReset finished!");
		});
	}

	@Command2.Subcommand(helpText = {
		"Version command",
		"Prints the plugin version"
	})
	public void version(CommandSender sender) {
		sender.sendMessage(VersionCommand.getVersion());
	}

	@Command2.Subcommand(helpText = {
		"Invite",
		"Shows an invite link to the server"
	})
	public void invite(CommandSender sender) {
		String invi = DiscordPlugin.plugin.inviteLink().get();
		if (invi.length() > 0) {
			sender.sendMessage("§bInvite link: " + invi);
			return;
		}
		DiscordPlugin.mainServer.getInvites().limitRequest(1)
			.switchIfEmpty(Mono.fromRunnable(() -> sender.sendMessage("§cNo invites found for the server.")))
			.subscribe(inv -> {
				sender.sendMessage("§bInvite link: https://discord.gg/" + inv.getCode());
			}, e -> sender.sendMessage("§cThe invite link is not set and the bot has no permission to get it."));
	}

	@Override
	public String[] getHelpText(Method method, Command2.Subcommand ann) {
		switch (method.getName()) {
			case "accept":
				return new String[]{ //
					"Accept Discord connection", //
					"Accept a pending connection between your Discord and Minecraft account.", //
					"To start the connection process, do §b/connect <MCname>§r in the " + DPUtils.botmention() + " channel on Discord", //
				};
			case "decline":
				return new String[]{ //
					"Decline Discord connection", //
					"Decline a pending connection between your Discord and Minecraft account.", //
					"To start the connection process, do §b/connect <MCname>§r in the " + DPUtils.botmention() + " channel on Discord", //
				};
			default:
				return super.getHelpText(method, ann);
		}
	}
}
