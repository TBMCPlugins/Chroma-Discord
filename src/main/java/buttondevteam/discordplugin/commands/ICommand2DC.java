package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.chat.ICommand2;
import lombok.Getter;
import lombok.val;

public abstract class ICommand2DC extends ICommand2 {
	public <T extends ICommand2> ICommand2DC() {
		super(DiscordPlugin.plugin.getManager());
		val ann = getClass().getAnnotation(CommandClass.class);
		if (ann == null)
			modOnly = false;
		else
			modOnly = ann.modOnly();
	}

	private final @Getter boolean modOnly;
}
