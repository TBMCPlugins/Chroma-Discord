package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.TBMCCommandBase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@CommandClass(path = "discord reset", modOnly = true)
public class ResetMCCommand extends TBMCCommandBase { //Not player-only, so not using DiscordMCCommandBase
    @Override
    public boolean OnCommand(CommandSender sender, String s, String[] strings) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
            sender.sendMessage("§bStopping MCChatListener...");
            DiscordPlugin.SafeMode = true;
            MCChatListener.stop(true);
            if (DiscordPlugin.dc.isLoggedIn()) {
                sender.sendMessage("§bLogging out...");
                DiscordPlugin.dc.logout();
            } else
                sender.sendMessage("§bWe're not logged in.");
            sender.sendMessage("§bLogging in...");
            DiscordPlugin.dc.login();
            DiscordPlugin.SafeMode = false;
            sender.sendMessage("§bChromaBot has been reset!");
        });
        return false;
    }

    @Override
    public String[] GetHelpText(String s) {
        return new String[0];
    }
}
