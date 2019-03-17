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
		if (message.length() == 0) return;
		message = DPUtils.sanitizeString(message);
		message = Character.toLowerCase(message.charAt(0)) + message.substring(1);
		this.message.reply(message);
	}

	@Override
	public void sendMessage(String[] message) {
		sendMessage(String.join("\n", message));
	}
}
