package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.lib.TBMCChannelConnectEvent;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;

@CommandClass(modOnly = false, path = "channelcon")
public class ChannelconMCCommand extends DiscordMCCommandBase {
    @Override //TODO: Since we require connecting the accounts, it can be done entirely on Discord.
    public boolean OnCommand(Player player, String alias, String[] args) {
        if (args.length < 1)
            return false;
        val chan = Channel.getChannels().stream().filter(ch -> ch.ID.equalsIgnoreCase(args[0]) || (ch.IDs != null && Arrays.stream(ch.IDs).anyMatch(cid -> cid.equalsIgnoreCase(args[0])))).findAny();
        if (!chan.isPresent()) {
            player.sendMessage("§cChannel with ID '" + args[0] + "' not found! The ID is the command for it without the /.");
            return true;
        }
        val dp = TBMCPlayer.getPlayer(player.getUniqueId(), TBMCPlayer.class).getAs(DiscordPlayer.class);
        if (dp == null) {
            player.sendMessage("§cYou need to connect your Discord account. In #bot do @ChromaBot connect " + player.getName());
            return true;
        }
        val ev = new TBMCChannelConnectEvent(player, chan.get());
        Bukkit.getPluginManager().callEvent(ev);
        if (ev.isCancelled() || ev.getGroupid() == null) {
            player.sendMessage("§cSorry, that didn't work. You cannot use that channel.");
            return true;
        }
        //MCChatListener.addCustomChat() - TODO: Call in Discord cmd
        PendingConnections.put(dp.getDiscordID(), new AbstractMap.SimpleEntry<>(ev.getChannel(), ev.getGroupid()));
        player.sendMessage("§bAlright! Now invite me to your server then show me the channel to use (@ChromaBot here).");
        return true;
    }

    @Override
    public String[] GetHelpText(String s) {
        return new String[]{//
                "§6---- Channel connect ---", //
                "This command allows you to connect a Minecraft channel to a Discord channel.", //
                "You need to have access to the MC channel and have manage permissions on the Discord channel." //
        };
    }

    /**
     * Key: Discord ID
     * Value of value. Channel Group ID
     */
    public static HashMap<String, AbstractMap.SimpleEntry<Channel, String>> PendingConnections = new HashMap<>();
}
