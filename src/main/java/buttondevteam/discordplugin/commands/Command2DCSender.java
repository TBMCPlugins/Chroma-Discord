package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.lib.chat.Command2Sender;
import discord4j.core.object.entity.Message;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;

@RequiredArgsConstructor
public class Command2DCSender implements Command2Sender {
	private final @Getter
	Message message;

	@Override
	public void sendMessage(String message) {
		if (message.length() == 0) return;
		message = DPUtils.sanitizeString(message);
		message = Character.toLowerCase(message.charAt(0)) + message.substring(1);
		val msg = message;
		this.message.getChannel().flatMap(ch -> ch.createMessage(this.message.getAuthorAsMember().a->
		a.getNicknameMention() + ", " + msg)))
	}

	@Override
	public void sendMessage(String[] message) {
		sendMessage(String.join("\n", message));
	}
}
