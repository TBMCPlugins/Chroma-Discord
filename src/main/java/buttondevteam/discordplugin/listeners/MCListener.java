package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.ConnectCommand;
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent;
import buttondevteam.lib.player.TBMCPlayerJoinEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerCommandEvent;
import sx.blah.discord.handle.obj.IUser;

public class MCListener implements Listener {
	@EventHandler
	public void onPlayerJoin(TBMCPlayerJoinEvent e) {
		if (ConnectCommand.WaitingToConnect.containsKey(e.GetPlayer().PlayerName().get())) {
			@SuppressWarnings("ConstantConditions") IUser user = DiscordPlugin.dc
				.getUserByID(Long.parseLong(ConnectCommand.WaitingToConnect.get(e.GetPlayer().PlayerName().get())));
			e.getPlayer().sendMessage("§bTo connect with the Discord account @" + user.getName() + "#" + user.getDiscriminator()
				+ " do /discord accept");
			e.getPlayer().sendMessage("§bIf it wasn't you, do /discord decline");
		}
	}

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
}
