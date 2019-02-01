package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.fun.FunModule;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.role.GameRoleModule;
import buttondevteam.lib.architecture.Component;
import lombok.val;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleCreateEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleUpdateEvent;
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent;

public class CommonListeners {

	/*
	MentionEvent:
	- CommandListener (starts with mention, only 'channelcon' and not in #bot)

	MessageReceivedEvent:
	- v CommandListener (starts with mention, in #bot or a connected chat)
	- Minecraft chat (is enabled in the channel and message isn't [/]mcchat)
	- CommandListener (with the correct prefix in #bot, or in private)
	*/
	public static IListener<?>[] getListeners() {
		return new IListener[]{new IListener<MessageReceivedEvent>() {
			@Override
			public void handle(MessageReceivedEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				if (FunModule.executeMemes(event.getMessage()))
					return;
				boolean handled = false;
				if (event.getChannel().getLongID() == DiscordPlugin.plugin.CommandChannel().get().getLongID() //If mentioned, that's higher than chat
						|| event.getMessage().getContent().contains("channelcon")) //Only 'channelcon' is allowed in other channels
					handled = CommandListener.runCommand(event.getMessage(), true); //#bot is handled here
				if (handled) return;
				val mcchat = Component.getComponents().get(MinecraftChatModule.class);
				if (mcchat != null && mcchat.isEnabled()) //ComponentManager.isEnabled() searches the component again
					handled = ((MinecraftChatModule) mcchat).getListener().handleDiscord(event); //Also runs Discord commands in chat channels
				if (!handled)
					handled = CommandListener.runCommand(event.getMessage(), false);
			}
		}, new IListener<sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent>() {
			@Override
			public void handle(PresenceUpdateEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				FunModule.handleFullHouse(event);
			}
		}, (IListener<RoleCreateEvent>) GameRoleModule::handleRoleEvent, //
			(IListener<RoleDeleteEvent>) GameRoleModule::handleRoleEvent, //
			(IListener<RoleUpdateEvent>) GameRoleModule::handleRoleEvent};
	}

    private static boolean debug = false;

    public static void debug(String debug) {
        if (CommonListeners.debug) //Debug
            DPUtils.getLogger().info(debug);
    }

    public static boolean debug() {
        return debug = !debug;
    }
}
