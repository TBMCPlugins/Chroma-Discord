package buttondevteam.discordplugin.commands;

import buttondevteam.core.component.channel.Channel;
import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.mcchat.MCChatCustom;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@CommandClass(helpText = {"---- Channel connect ---", //
	"This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
	"You need to have access to the MC channel and have manage permissions on the Discord channel.", //
	"You also need to have your Minecraft account connected. In #bot use /connect <mcname>.", //
	"Call this command from the channel you want to use.", //
	"Usage: @Bot channelcon <mcchannel>", //
	"Use the ID (command) of the channel, for example `g` for the global chat.", //
	"To remove a connection use @ChromaBot channelcon remove in the channel.", //
	"Mentioning the bot is needed in this case because the / prefix only works in #bot.", //
	"Invite link: <https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot&permissions=268509264>" //
})
public class ChannelconCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean remove(Command2DCSender sender) {
		val message = sender.getMessage();
		if (checkPerms(message)) return true;
		if (MCChatCustom.removeCustomChat(message.getChannel()))
			message.reply("channel connection removed.");
		else
			message.reply("this channel isn't connected.");
		return true;
	}

	@Command2.Subcommand
	public boolean toggle(Command2DCSender sender, @Command2.OptionalArg String toggle) {
		val message = sender.getMessage();
		if (checkPerms(message)) return true;
		val cc = MCChatCustom.getCustomChat(message.getChannel());
		assert cc != null; //It's not null
		Supplier<String> togglesString = () -> Arrays.stream(ChannelconBroadcast.values()).map(t -> t.toString().toLowerCase() + ": " + ((cc.toggles & t.flag) == 0 ? "disabled" : "enabled")).collect(Collectors.joining("\n"))
			+ "\n\n" + TBMCSystemChatEvent.BroadcastTarget.stream().map(TBMCSystemChatEvent.BroadcastTarget::getName).collect(Collectors.joining("\n"));
		if (toggle == null) {
			message.reply("toggles:\n" + togglesString.get());
			return true;
		}
		String arg = toggle.toUpperCase();
		val b = Arrays.stream(ChannelconBroadcast.values()).filter(t -> t.toString().equals(arg)).findAny();
		if (!b.isPresent()) {
			val bt = TBMCSystemChatEvent.BroadcastTarget.get(arg);
			if (bt == null) {
				message.reply("cannot find toggle. Toggles:\n" + togglesString.get());
				return true;
			}
			final boolean add;
			if (add = !cc.brtoggles.contains(bt))
				cc.brtoggles.add(bt);
			else
				cc.brtoggles.remove(bt);
			return respond(sender, "'" + bt.getName() + "' " + (add ? "en" : "dis") + "abled");
		}
		//A B | F
		//------- A: original - B: mask - F: new
		//0 0 | 0
		//0 1 | 1
		//1 0 | 1
		//1 1 | 0
		// XOR
		cc.toggles ^= b.get().flag;
		message.reply("'" + b.get().toString().toLowerCase() + "' " + ((cc.toggles & b.get().flag) == 0 ? "disabled" : "enabled"));
		return true;
	}

	@Command2.Subcommand
	public boolean def(Command2DCSender sender, String args) {
		val message = sender.getMessage();
		if (checkPerms(message)) return true;
		if (MCChatCustom.hasCustomChat(message.getChannel()))
			return respond(sender, "this channel is already connected to a Minecraft channel. Use `@ChromaBot channelcon remove` to remove it.");
	    val chan = Channel.getChannels().filter(ch -> ch.ID.equalsIgnoreCase(args) || (Arrays.stream(ch.IDs().get()).anyMatch(cid -> cid.equalsIgnoreCase(args)))).findAny();
        if (!chan.isPresent()) { //TODO: Red embed that disappears over time (kinda like the highlight messages in OW)
            message.reply("MC channel with ID '" + args + "' not found! The ID is the command for it without the /.");
            return true;
        }
        val dp = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class);
        val chp = dp.getAs(TBMCPlayer.class);
        if (chp == null) {
	        message.reply("you need to connect your Minecraft account. On our server in " + DPUtils.botmention() + " do " + DiscordPlugin.getPrefix() + "connect <MCname>");
            return true;
        }
        DiscordConnectedPlayer dcp = new DiscordConnectedPlayer(message.getAuthor(), message.getChannel(), chp.getUUID(), Bukkit.getOfflinePlayer(chp.getUUID()).getName());
        //Using a fake player with no login/logout, should be fine for this event
	    String groupid = chan.get().getGroupID(dcp);
        if (groupid == null) {
            message.reply("sorry, that didn't work. You cannot use that Minecraft channel.");
            return true;
        }
        /*if (MCChatListener.getCustomChats().stream().anyMatch(cc -> cc.groupID.equals(groupid) && cc.mcchannel.ID.equals(chan.get().ID))) {
            message.reply("sorry, this MC chat is already connected to a different channel, multiple channels are not supported atm.");
            return true;
        }*/ //TODO: "Channel admins" that can connect channels?
		MCChatCustom.addCustomChat(message.getChannel(), groupid, chan.get(), message.getAuthor(), dcp, 0, new ArrayList<>());
        message.reply("alright, connection made to group `" + groupid + "`!");
        return true;
    }

	private boolean checkPerms(IMessage message) {
		if (!PermissionUtils.hasPermissions(message.getChannel(), message.getAuthor(), Permissions.MANAGE_CHANNEL)) {
			message.reply("you need to have manage permissions for this channel!");
			return true;
		}
		return false;
	}

	@Override
	public String[] getHelpText(Method method) {
        return new String[]{ //
                "---- Channel connect ---", //
                "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
                "You need to have access to the MC channel and have manage permissions on the Discord channel.", //
	        "You also need to have your Minecraft account connected. In " + DPUtils.botmention() + " use " + DiscordPlugin.getPrefix() + "connect <mcname>.", //
		        "Call this command from the channel you want to use.", //
		        "Usage: @" + DiscordPlugin.dc.getOurUser().getName() + " channelcon <mcchannel>", //
		        "Use the ID (command) of the channel, for example `g` for the global chat.", //
                "To remove a connection use @ChromaBot channelcon remove in the channel.", //
	        "Mentioning the bot is needed in this case because the " + DiscordPlugin.getPrefix() + " prefix only works in " + DPUtils.botmention() + ".", //
                "Invite link: <https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot&permissions=268509264>" //
        };
    }
}
