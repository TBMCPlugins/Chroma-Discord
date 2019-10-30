package buttondevteam.discordplugin;

import buttondevteam.discordplugin.playerfaker.VCMDWrapper;
import org.bukkit.entity.Player;

public interface IMCPlayer<T> extends Player {
	VCMDWrapper getVanillaCmdListener();
}
