package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.lib.chat.Command2Sender;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import sx.blah.discord.handle.obj.IMessage;

@RequiredArgsConstructor
public class Command2DCSender implements Command2Sender {
	private final @Getter IMessage message;

	@Override
	public void sendMessage(String message) {
		this.message.reply(DPUtils.sanitizeString(message));
	}

	@Override
	public void sendMessage(String[] message) {
		this.message.reply(DPUtils.sanitizeString(String.join("\n", message)));
	}
}
