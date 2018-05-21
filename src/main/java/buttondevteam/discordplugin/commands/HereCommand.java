package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.mccommands.ChannelconMCCommand;
import lombok.val;
import sx.blah.discord.handle.obj.IMessage;

public class HereCommand extends DiscordCommandBase {
    @Override
    public String getCommandName() {
        return "here";
    }

    @Override
    public void run(IMessage message, String args) {
        val chgroup = ChannelconMCCommand.PendingConnections.get(message.getAuthor().getStringID());
        if (chgroup == null) {
            message.reply("no pending connection found! "); //TODO
        }
    }

    @Override
    public String[] getHelpText() {
        return new String[0];
    }
}
