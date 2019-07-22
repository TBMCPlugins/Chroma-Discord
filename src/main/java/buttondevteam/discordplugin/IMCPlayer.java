package buttondevteam.discordplugin;

import buttondevteam.discordplugin.playerfaker.VCMDWrapper;
import org.bukkit.entity.Player;

public interface IMCPlayer<T extends DiscordSenderBase & IMCPlayer<T>> extends Player {
	VCMDWrapper<T> getVanillaCmdListener();
}
