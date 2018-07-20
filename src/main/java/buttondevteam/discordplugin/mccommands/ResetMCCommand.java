package buttondevteam.discordplugin.mccommands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.TBMCCommandBase;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

@CommandClass(path = "discord reset", modOnly = true)
public class ResetMCCommand extends TBMCCommandBase { //Not player-only, so not using DiscordMCCommandBase
    public static boolean resetting = false;
    @Override
    public boolean OnCommand(CommandSender sender, String s, String[] strings) {
        Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> {
            resetting = true; //Turned off after sending enable message (ReadyEvent)
            sender.sendMessage("§bDisabling DiscordPlugin...");
            Bukkit.getPluginManager().disablePlugin(DiscordPlugin.plugin);
            sender.sendMessage("§bEnabling DiscordPlugin...");
            Bukkit.getPluginManager().enablePlugin(DiscordPlugin.plugin);
            sender.sendMessage("§bReset finished!");
        });
        return true;
    }

    @Override
    public String[] GetHelpText(String s) {
        return new String[]{ //
                "§6---- Reset ChromaBot ----", //
                "This command stops the Minecraft chat and relogs the bot." //
        };
    }
}
