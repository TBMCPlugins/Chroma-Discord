package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.ChannelconBroadcast;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent;
import buttondevteam.lib.player.TBMCYEEHAWEvent;
import com.earth2me.essentials.CommandSource;
import net.ess3.api.events.MuteStatusChangeEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.BroadcastMessageEvent;
import org.bukkit.event.server.ServerCommandEvent;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.MissingPermissionsException;

public class MCListener implements Listener {
    @EventHandler
    public void onGetInfo(TBMCPlayerGetInfoEvent e) {
        if (DiscordPlugin.SafeMode)
            return;
        DiscordPlayer dp = e.getPlayer().getAs(DiscordPlayer.class);
        if (dp == null || dp.getDiscordID() == null || dp.getDiscordID().equals(""))
            return;
        IUser user = DiscordPlugin.dc.getUserByID(Long.parseLong(dp.getDiscordID()));
        e.addInfo("Discord tag: " + user.getName() + "#" + user.getDiscriminator());
        e.addInfo(user.getPresence().getStatus().toString());
        if (user.getPresence().getActivity().isPresent() && user.getPresence().getText().isPresent())
            e.addInfo(user.getPresence().getActivity().get() + ": " + user.getPresence().getText().get());
    }

    @EventHandler
    public void onServerCommand(ServerCommandEvent e) {
        DiscordPlugin.Restart = !e.getCommand().equalsIgnoreCase("stop"); // The variable is always true except if stopped
    }

    @EventHandler
    public void onPlayerMute(MuteStatusChangeEvent e) {
        try {
            DPUtils.performNoWait(() -> {
                final IRole role = DiscordPlugin.dc.getRoleByID(164090010461667328L);
                final CommandSource source = e.getAffected().getSource();
                if (!source.isPlayer())
                    return;
	            final DiscordPlayer p = TBMCPlayerBase.getPlayer(source.getPlayer().getUniqueId(), TBMCPlayer.class)
			            .getAs(DiscordPlayer.class);
	            if (p == null) return;
                final IUser user = DiscordPlugin.dc.getUserByID(
		                Long.parseLong(p.getDiscordID()));
                if (e.getValue())
                    user.addRole(role);
                else
                    user.removeRole(role);
	            DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, (e.getValue() ? "M" : "Unm") + "uted user: " + user.getName());
            });
        } catch (DiscordException | MissingPermissionsException ex) {
            TBMCCoreAPI.SendException("Failed to give/take Muted role to player " + e.getAffected().getName() + "!",
                    ex);
        }
    }

    @EventHandler
    public void onChatSystemMessage(TBMCSystemChatEvent event) {
	    MCChatUtils.forAllowedMCChat(MCChatUtils.send(event.getMessage()), event);
    }

    @EventHandler
    public void onBroadcastMessage(BroadcastMessageEvent event) {
	    MCChatUtils.forCustomAndAllMCChat(MCChatUtils.send(event.getMessage()), ChannelconBroadcast.BROADCAST, false);
    }

    @EventHandler
    public void onYEEHAW(TBMCYEEHAWEvent event) { //TODO: Inherit from the chat event base to have channel support
        String name = event.getSender() instanceof Player ? ((Player) event.getSender()).getDisplayName()
                : event.getSender().getName();
	    //Channel channel = ChromaGamerBase.getFromSender(event.getSender()).channel().get(); - TODO
	    MCChatUtils.forAllMCChat(MCChatUtils.send(name + " <:YEEHAW:" + DiscordPlugin.mainServer.getEmojiByName("YEEHAW").getStringID() + ">s"));
    }
}
