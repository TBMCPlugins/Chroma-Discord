package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.TBMCChannelConnectEvent;
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
        return "here";
    }

    @Override
    public boolean run(IMessage message, String args) {
        if (args.length() == 0)
            return false;
        if (!PermissionUtils.hasPermissions(message.getChannel(), message.getAuthor(), Permissions.MANAGE_CHANNEL)) {
            message.reply("you need to have manage permissions for this channel!");
            return true;
        }
        //TODO: What if they no longer have permission to view the channel
        if (MCChatListener.hasCustomChat(message.getChannel())) { //TODO: Remove command
            message.reply("this channel is already connected to a Minecraft channel.");
            return true;
        }
        val chan = Channel.getChannels().stream().filter(ch -> ch.ID.equalsIgnoreCase(args) || (ch.IDs != null && Arrays.stream(ch.IDs).anyMatch(cid -> cid.equalsIgnoreCase(args)))).findAny();
        if (!chan.isPresent()) { //TODO: Red embed that disappears over time (kinda like the highlight messages in OW)
            message.reply("MC channel with ID '" + args + "' not found! The ID is the command for it without the /.");
            return true;
        }
        val chp = DiscordPlayer.getUser(message.getAuthor().getStringID(), DiscordPlayer.class).getAs(TBMCPlayer.class);
        if (chp == null) {
            message.reply("you need to connect your Minecraft account. In #bot do @ChromaBot connect <MCname>");
            return true;
        }
        val ev = new TBMCChannelConnectEvent(new DiscordConnectedPlayer(message.getAuthor(), message.getChannel(), chp.getUUID(), Bukkit.getOfflinePlayer(chp.getUUID()).getName()), chan.get());
        Bukkit.getPluginManager().callEvent(ev); //Using a fake player with no login/logout, should be fine for this event
        if (ev.isCancelled() || ev.getGroupid() == null) {
            message.reply("sorry, that didn't work. You cannot use that Minecraft channel.");
            return true;
        }
        MCChatListener.addCustomChat(message.getChannel(), args, ev.getChannel());
        message.reply("alright, connection made!");
        return true;
    }

    @Override
    public String[] getHelpText() {
        return new String[]{ //
                "§6---- Channel connect ---", //
                "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
                "You need to have access to the MC channel and have manage permissions on the Discord channel.", //
                "You also need to have your Minecraft account connected. In #bot use @ChromaBot connect <mcname>.", //
                "Call this command from the channel you want to use. Usage: @ChromaBot channelcon <mcchannel>", //
                "Invite link: https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot" //
        };
    }
}
