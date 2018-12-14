package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.listeners.CommonListeners;
import sx.blah.discord.handle.obj.IMessage;

public class DebugCommand extends DiscordCommandBase {
    @Override
    public String getCommandName() {
        return "debug";
    }

    @Override
    public boolean run(IMessage message, String args) {
        if (message.getAuthor().hasRole(DiscordPlugin.mainServer.getRoleByID(126030201472811008L)))
	        message.reply("Debug " + (CommonListeners.debug() ? "enabled" : "disabled"));
        else
            message.reply("You need to be a moderator to use this command.");
        return true;
    }

    @Override
    public String[] getHelpText() {
        return new String[]{"Switches debug mode."};
    }
}
