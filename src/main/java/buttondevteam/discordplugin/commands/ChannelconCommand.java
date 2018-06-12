package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.TBMCChannelConnectFakeEvent;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.Permissions;
import sx.blah.discord.util.PermissionUtils;

import java.util.Arrays;

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
        //TODO: What if they no longer have permission to view the channel - check on some message events and startup - if somebody who can view the channel (on both platforms) has their accounts connected, keep it
        if (MCChatListener.hasCustomChat(message.getChannel())) {
            if (args.equalsIgnoreCase("remove")) {
                if (MCChatListener.removeCustomChat(message.getChannel()))
                    message.reply("channel connection removed.");
                else
                    message.reply("wait what, couldn't remove channel connection.");
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
        val ev = new TBMCChannelConnectFakeEvent(new DiscordConnectedPlayer(message.getAuthor(), message.getChannel(), chp.getUUID(), Bukkit.getOfflinePlayer(chp.getUUID()).getName()), chan.get());
        //Using a fake player with no login/logout, should be fine for this event
        String groupid = ev.getGroupID(ev.getSender()); //We're not trying to send in a specific group, we want to know which group the user belongs to (so not getGroupID())
        if (groupid == null) {
            message.reply("sorry, that didn't work. You cannot use that Minecraft channel.");
            return true;
        }
        MCChatListener.addCustomChat(message.getChannel(), args, ev.getChannel(), dp, message.getAuthor()); //TODO: SAVE
        message.reply("alright, connection made to group `" + groupid + "`!");
        return true;
    }

    @Override
    public String[] getHelpText() {
        return new String[]{ //
                "---- Channel connect ---", //
                "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
                "You need to have access to the MC channel and have manage permissions on the Discord channel.", //
                "You also need to have your Minecraft account connected. In #bot use /connect <mcname>.", //
                "Call this command from the channel you want to use. Usage: @ChromaBot channelcon <mcchannel>", //
                "To remove a connection use @ChromaBot channelcon remove in the channel.", //
                "Mentioning the bot is needed in this case because the / prefix only works in #bot.", //
                "Invite link: <https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot&permissions=268509264>" //
        };
    }
}
