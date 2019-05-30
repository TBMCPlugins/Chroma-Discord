package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.fun.FunModule;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.role.GameRoleModule;
import buttondevteam.discordplugin.util.Timings;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import discord4j.core.event.EventDispatcher;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.PrivateChannel;
import lombok.val;
import reactor.core.publisher.Mono;

public class CommonListeners {

	public static final Timings timings = new Timings();

	/*
	MentionEvent:
	- CommandListener (starts with mention, only 'channelcon' and not in #bot)

	MessageReceivedEvent:
	- v CommandListener (starts with mention, in #bot or a connected chat)
	- Minecraft chat (is enabled in the channel and message isn't [/]mcchat)
	- CommandListener (with the correct prefix in #bot, or in private)
	*/
	public static void register(EventDispatcher dispatcher) {
		dispatcher.on(MessageCreateEvent.class).flatMap(event -> {
			timings.printElapsed("Message received");
			val def = Mono.empty();
			if (DiscordPlugin.SafeMode)
				return def;
			val author = event.getMessage().getAuthor();
			if (!author.isPresent() || author.get().isBot())
				return def;
			if (FunModule.executeMemes(event.getMessage()))
				return def;
			val commandChannel = DiscordPlugin.plugin.commandChannel().get();
			val commandCh = DPUtils.getMessageChannel(DiscordPlugin.plugin.commandChannel());
			return commandCh.filterWhen(ch -> event.getMessage().getChannel().map(mch ->
				commandChannel != null && event.getMessage().getChannelId().asLong() == commandChannel.asLong() //If mentioned, that's higher than chat
					|| mch instanceof PrivateChannel
					|| event.getMessage().getContent().orElse("").contains("channelcon"))) //Only 'channelcon' is allowed in other channels
				.filterWhen(ch -> { //Only continue if this doesn't handle the event
					timings.printElapsed("Run command 1");
					return CommandListener.runCommand(event.getMessage(), ch, true); //#bot is handled here
				}).filterWhen(ch -> {
					timings.printElapsed("mcchat");
					val mcchat = Component.getComponents().get(MinecraftChatModule.class);
					if (mcchat != null && mcchat.isEnabled()) //ComponentManager.isEnabled() searches the component again
						return ((MinecraftChatModule) mcchat).getListener().handleDiscord(event); //Also runs Discord commands in chat channels
					return Mono.empty(); //Wasn't handled, continue
				}).filterWhen(ch -> {
					timings.printElapsed("Run command 2");
					return CommandListener.runCommand(event.getMessage(), ch, false);
				});
		}).onErrorContinue((err, obj) -> TBMCCoreAPI.SendException("An error occured while handling a message!", err))
			.subscribe();
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
