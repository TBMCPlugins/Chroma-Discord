package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.ChannelconBroadcast;
import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.mcchat.MCChatCustom;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;

import java.util.Arrays;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ChannelconCommand extends DiscordCommandBase {
    @Override
    public String getCommandName() {
        return "channelcon";
    }

    @Override
    public boolean run(IMessage message, String args) {
        if (args.length() == 0)
            return false;
        if (!PermissionUtils.hasPermissions(message.getChannel(), message.getAuthor(), Permissions.MANAGE_CHANNEL)) {
            message.reply("you need to have manage permissions for this channel!");
            return true;
        }
	    if (MCChatCustom.hasCustomChat(message.getChannel())) {
	        if (args.toLowerCase().startsWith("remove")) {
		        if (MCChatCustom.removeCustomChat(message.getChannel()))
                    message.reply("channel connection removed.");
                else
                    message.reply("wait what, couldn't remove channel connection.");
                return true;
            }
	        if (args.toLowerCase().startsWith("toggle")) {
		        val cc = MCChatCustom.getCustomChat(message.getChannel());
		        Supplier<String> togglesString = () -> Arrays.stream(ChannelconBroadcast.values()).map(t -> t.toString().toLowerCase() + ": " + ((cc.toggles & t.flag) == 0 ? "disabled" : "enabled")).collect(Collectors.joining("\n"));
		        String[] argsa = args.split(" ");
		        if (argsa.length < 2) {
			        message.reply("toggles:\n" + togglesString.get());
			        return true;
		        }
		        String arg = argsa[1].toUpperCase();
		        val b = Arrays.stream(ChannelconBroadcast.values()).filter(t -> t.toString().equals(arg)).findAny();
		        if (!b.isPresent()) {
			        message.reply("cannot find toggle. Toggles:\n" + togglesString.get());
			        return true;
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
            message.reply("this channel is already connected to a Minecraft channel. Use `@ChromaBot channelcon remove` to remove it.");
            return true;
        }
        val chan = Channel.getChannels().stream().filter(ch -> ch.ID.equalsIgnoreCase(args) || (ch.IDs != null && Arrays.stream(ch.IDs).anyMatch(cid -> cid.equalsIgnoreCase(args)))).findAny();
        if (!chan.isPresent()) { //TODO: Red embed that disappears over time (kinda like the highlight messages in OW)
            message.reply("MC channel with ID '" + args + "' not found! The ID is the command for it without the /.");
            return true;
        }
        val dp = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class);
        val chp = dp.getAs(TBMCPlayer.class);
        if (chp == null) {
            message.reply("you need to connect your Minecraft account. On our server in #bot do /connect <MCname>");
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
	    MCChatCustom.addCustomChat(message.getChannel(), groupid, chan.get(), message.getAuthor(), dcp, 0);
        message.reply("alright, connection made to group `" + groupid + "`!");
        return true;
    }

    @Override
    public String[] getHelpText() {
        return new String[]{ //
                "---- Channel connect ---", //
                "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
                "You need to have access to the MC channel and have manage permissions on the Discord channel.", //
		        "You also need to have your Minecraft account connected. In #bot use " + DiscordPlugin.getPrefix() + "connect <mcname>.", //
		        "Call this command from the channel you want to use.", //
		        "Usage: @" + DiscordPlugin.dc.getOurUser().getName() + " channelcon <mcchannel>", //
		        "Use the ID (command) of the channel, for example `g` for the global chat.", //
                "To remove a connection use @ChromaBot channelcon remove in the channel.", //
		        "Mentioning the bot is needed in this case because the " + DiscordPlugin.getPrefix() + " prefix only works in #bot.", //
                "Invite link: <https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot&permissions=268509264>" //
        };
    }
}
