package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import reactor.core.publisher.Mono;

@CommandClass(helpText = {
	"Switches debug mode."
})
public class DebugCommand extends ICommand2DC {
	@Command2.Subcommand
	public boolean def(Command2DCSender sender) {
		sender.getMessage().getAuthorAsMember()
			.switchIfEmpty(sender.getMessage().getAuthor() //Support DMs
				.map(u -> u.asMember(DiscordPlugin.mainServer.getId()))
				.orElse(Mono.empty()))
			.flatMap(m -> DiscordPlugin.plugin.modRole().get()
				.map(mr -> m.getRoleIds().stream().anyMatch(r -> r.equals(mr.getId()))))
			.subscribe(success -> {
				if (success)
					sender.sendMessage("debug " + (CommonListeners.debug() ? "enabled" : "disabled"));
				else
					sender.sendMessage("you need to be a moderator to use this command.");
			});
		return true;
	}
}
