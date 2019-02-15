package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.chat.Command2;

import java.util.HashMap;
import java.util.function.Function;

public class Command2DC extends Command2<ICommand2DC, Command2DCSender> {
	private HashMap<String, SubcommandData<ICommand2DC>> subcommands = new HashMap<>();
	private HashMap<Class<?>, ParamConverter<?>> paramConverters = new HashMap<>();

	@Override
	public <T> void addParamConverter(Class<T> cl, Function<String, T> converter, String errormsg) {
		addParamConverter(cl, converter, errormsg, paramConverters);
	}

	@Override
	public boolean handleCommand(Command2DCSender sender, String commandLine) throws Exception {
		return handleCommand(sender, commandLine, subcommands, paramConverters);
	}

	@Override
	public void registerCommand(ICommand2DC command) {
		registerCommand(command, subcommands, DiscordPlugin.getPrefix()); //Needs to be configurable for the helps
	}

	@Override
	public boolean hasPermission(Command2DCSender sender, ICommand2DC command) {
		return !command.isModOnly() || sender.getMessage().getAuthor().hasRole(DiscordPlugin.plugin.ModRole().get());
	}
}
