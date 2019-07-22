package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class VCMDWrapper<T extends DiscordSenderBase & IMCPlayer<T>> {
	@Getter //Needed to mock the player
	private final VanillaCommandListener<T> listener;
}
