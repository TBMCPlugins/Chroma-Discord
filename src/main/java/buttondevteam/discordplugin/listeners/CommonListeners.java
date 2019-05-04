package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.fun.FunModule;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.role.GameRoleModule;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import lombok.val;

public class CommonListeners {

	/*
	MentionEvent:
	- CommandListener (starts with mention, only 'channelcon' and not in #bot)

	MessageReceivedEvent:
	- v CommandListener (starts with mention, in #bot or a connected chat)
	- Minecraft chat (is enabled in the channel and message isn't [/]mcchat)
	- CommandListener (with the correct prefix in #bot, or in private)
	*/
	public static void register(EventDispatcher dispatcher) {
		dispatcher.on(MessageCreateEvent.class).subscribe(event -> {
			if (DiscordPlugin.SafeMode)
				return;
			val author = event.getMessage().getAuthor();
			if (!author.isPresent() || author.get().isBot())
				return;
			//System.out.println("Author: "+author.get());
			//System.out.println("Bot: "+author.get().isBot());
			if (FunModule.executeMemes(event.getMessage()))
				return;
			try {
				boolean handled = false;
				val commandChannel = DiscordPlugin.plugin.CommandChannel().get();
				if ((commandChannel != null && event.getMessage().getChannelId().asLong() == commandChannel.asLong()) //If mentioned, that's higher than chat
					|| event.getMessage().getContent().orElse("").contains("channelcon")) //Only 'channelcon' is allowed in other channels
					handled = CommandListener.runCommand(event.getMessage(), true); //#bot is handled here
				if (handled) return;
				//System.out.println("Message handling");
				val mcchat = Component.getComponents().get(MinecraftChatModule.class);
				if (mcchat != null && mcchat.isEnabled()) //ComponentManager.isEnabled() searches the component again
					handled = ((MinecraftChatModule) mcchat).getListener().handleDiscord(event); //Also runs Discord commands in chat channels
				if (!handled)
					handled = CommandListener.runCommand(event.getMessage(), false);
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while handling a message!", e);
			}
		});
		dispatcher.on(PresenceUpdateEvent.class).subscribe(event -> {
			if (DiscordPlugin.SafeMode)
				return;
			FunModule.handleFullHouse(event);
		});
		dispatcher.on(RoleCreateEvent.class).subscribe(GameRoleModule::handleRoleEvent);
		dispatcher.on(RoleDeleteEvent.class).subscribe(GameRoleModule::handleRoleEvent);
		dispatcher.on(RoleUpdateEvent.class).subscribe(GameRoleModule::handleRoleEvent);
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
