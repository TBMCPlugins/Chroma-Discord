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
import reactor.core.publisher.Mono;

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
		dispatcher.on(MessageCreateEvent.class).flatMap(event -> {
			val def = Mono.empty();
			if (DiscordPlugin.SafeMode)
				return def;
			val author = event.getMessage().getAuthor();
			if (!author.isPresent() || author.get().isBot())
				return def;
			//System.out.println("Author: "+author.get());
			//System.out.println("Bot: "+author.get().isBot());
			if (FunModule.executeMemes(event.getMessage()))
				return def;
			val commandChannel = DiscordPlugin.plugin.CommandChannel().get();
			val commandCh = DPUtils.getMessageChannel(DiscordPlugin.plugin.CommandChannel());
			return commandCh.filter(ch -> (commandChannel != null && event.getMessage().getChannelId().asLong() == commandChannel.asLong()) //If mentioned, that's higher than chat
				|| event.getMessage().getContent().orElse("").contains("channelcon")) //Only 'channelcon' is allowed in other channels
				.filterWhen(ch -> { //Only continue if this doesn't handle the event
					return CommandListener.runCommand(event.getMessage(), ch, true); //#bot is handled here
				}).filterWhen(ch -> {
					val mcchat = Component.getComponents().get(MinecraftChatModule.class);
					if (mcchat != null && mcchat.isEnabled()) //ComponentManager.isEnabled() searches the component again
						return ((MinecraftChatModule) mcchat).getListener().handleDiscord(event); //Also runs Discord commands in chat channels
					return Mono.empty(); //Wasn't handled, continue
				}).filterWhen(ch -> CommandListener.runCommand(event.getMessage(), ch, false));
		}).onErrorContinue((err, obj) -> TBMCCoreAPI.SendException("An error occured while handling a message!", err))
			.subscribe();
		/*dispatcher.on(MessageCreateEvent.class).doOnNext(x -> System.out.println("Got message"))
			.flatMap(MessageCreateEvent::getGuild)
			.flatMap(guild -> DiscordPlugin.dc.getSelf())
			.flatMap(self -> self.asMember(DiscordPlugin.mainServer.getId()))
			.flatMap(Member::getRoles).subscribe(roles -> System.out.println("Roles: " + roles));*/
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
