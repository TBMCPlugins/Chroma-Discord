package buttondevteam.discordplugin;

import org.bukkit.entity.Player;

import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;

public interface IMCPlayer<T extends DiscordSenderBase & IMCPlayer<T>> extends Player {
	VanillaCommandListener<T> getVanillaCmdListener();
}
