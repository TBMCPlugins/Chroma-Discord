package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.playerfaker.VCMDWrapper;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.lang.reflect.Modifier;

public abstract class DiscordPlayerSender extends DiscordSenderBase implements IMCPlayer<DiscordPlayerSender> {

	protected Player player;
	private @Getter VCMDWrapper vanillaCmdListener;

	public DiscordPlayerSender(User user, MessageChannel channel, Player player, MinecraftChatModule module) {
		super(user, channel);
		this.player = player;
		vanillaCmdListener = new VCMDWrapper(VCMDWrapper.createListener(this, player, module));
	}

	@Override
	public void sendMessage(String message) {
		player.sendMessage(message);
		super.sendMessage(message);
	}

	@Override
	public void sendMessage(String[] messages) {
		player.sendMessage(messages);
		super.sendMessage(messages);
	}

	public static DiscordPlayerSender create(User user, MessageChannel channel, Player player, MinecraftChatModule module) {
		return Mockito.mock(DiscordPlayerSender.class, Mockito.withSettings().stubOnly().defaultAnswer(invocation -> {
			if (!Modifier.isAbstract(invocation.getMethod().getModifiers()))
				return invocation.callRealMethod();
			return invocation.getMethod().invoke(((DiscordPlayerSender) invocation.getMock()).player, invocation.getArguments());
		}).useConstructor(user, channel, player, module));
	}
}
