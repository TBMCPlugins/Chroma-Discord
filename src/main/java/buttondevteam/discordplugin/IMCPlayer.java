package buttondevteam.discordplugin;

import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import org.bukkit.entity.Player;

public interface IMCPlayer<T extends DiscordSenderBase & IMCPlayer<T>> extends Player {
	VanillaCommandListener<T> getVanillaCmdListener();
}
