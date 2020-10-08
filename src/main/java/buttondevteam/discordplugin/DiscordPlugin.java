package buttondevteam.discordplugin;

import buttondevteam.discordplugin.announcer.AnnouncerModule;
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.discordplugin.commands.*;
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule;
import buttondevteam.discordplugin.fun.FunModule;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.discordplugin.listeners.MCListener;
import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.mccommands.DiscordMCCommand;
import buttondevteam.discordplugin.role.GameRoleModule;
import buttondevteam.discordplugin.util.Timings;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.ButtonPlugin;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import buttondevteam.lib.player.ChromaGamerBase;
import com.google.common.io.Files;
import discord4j.common.util.Snowflake;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.GatewayDiscordClient;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.rest.util.Color;
import discord4j.store.jdk.JdkStoreService;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.mockito.internal.util.MockUtil;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ButtonPlugin.ConfigOpts(disableConfigGen = true)
public class DiscordPlugin extends ButtonPlugin {
	public static GatewayDiscordClient dc;
	public static DiscordPlugin plugin;
	public static boolean SafeMode = true;
	@Getter
	private Command2DC manager;

	/**
	 * The prefix to use with Discord commands like /role. It only works in the bot channel.
	 */
	private ConfigData<Character> prefix() {
		return getIConfig().getData("prefix", '/', str -> ((String) str).charAt(0), Object::toString);
	}

	public static char getPrefix() {
		if (plugin == null) return '/';
		return plugin.prefix().get();
	}

	/**
	 * The main server where the roles and other information is pulled from. It's automatically set to the first server the bot's invited to.
	 */
	private ConfigData<Optional<Guild>> mainServer() {
		return getIConfig().getDataPrimDef("mainServer", 0L,
			id -> {
				//It attempts to get the default as well
				if ((long) id == 0L)
					return Optional.empty(); //Hack?
				return dc.getGuildById(Snowflake.of((long) id))
					.onErrorResume(t -> Mono.fromRunnable(() -> getLogger().warning("Failed to get guild: " + t.getMessage()))).blockOptional();
			},
			g -> g.map(gg -> gg.getId().asLong()).orElse(0L));
	}

	/**
	 * The (bot) channel to use for Discord commands like /role.
	 */
	public ConfigData<Snowflake> commandChannel() {
		return DPUtils.snowflakeData(getIConfig(), "commandChannel", 0L);
	}

	/**
	 * The role that allows using mod-only Discord commands.
	 * If empty (''), then it will only allow for the owner.
	 */
	public ConfigData<Mono<Role>> modRole() {
		return DPUtils.roleData(getIConfig(), "modRole", "Moderator");
	}

	/**
	 * The invite link to show by /discord invite. If empty, it defaults to the first invite if the bot has access.
	 */
	public ConfigData<String> inviteLink() {
		return getIConfig().getData("inviteLink", "");
	}

	@Override
	public void onLoad() { //Needed by ServerWatcher
		var thread = Thread.currentThread();
		var cl = thread.getContextClassLoader();
		thread.setContextClassLoader(getClassLoader());
		MockUtil.isMock(null); //Load MockUtil to load Mockito plugins
		thread.setContextClassLoader(cl);
		getLogger().info("Load complete");
	}

	@Override
	public void pluginEnable() {
		try {
			getLogger().info("Initializing...");
			plugin = this;
			manager = new Command2DC();
			registerCommand(new DiscordMCCommand()); //Register so that the reset command works
			String token;
			File tokenFile = new File("TBMC", "Token.txt");
			if (tokenFile.exists()) //Legacy support
				//noinspection UnstableApiUsage
				token = Files.readFirstLine(tokenFile, StandardCharsets.UTF_8);
			else {
				File privateFile = new File(getDataFolder(), "private.yml");
				val conf = YamlConfiguration.loadConfiguration(privateFile);
				token = conf.getString("token");
				if (token == null || token.equalsIgnoreCase("Token goes here")) {
					conf.set("token", "Token goes here");
					conf.save(privateFile);

					getLogger().severe("Token not found! Please set it in private.yml then do /discord reset");
					getLogger().severe("You need to have a bot account to use with your server.");
					getLogger().severe("If you don't have one, go to https://discordapp.com/developers/applications/ and create an application, then create a bot for it and copy the bot token.");
					return;
				}
			}
			val cb = DiscordClientBuilder.create(token).build().gateway();
			cb.setInitialStatus(si -> Presence.doNotDisturb(Activity.playing("booting")));
			cb.setStoreService(new JdkStoreService()); //The default doesn't work for some reason - it's waaay faster now
			cb.login().subscribe(dc -> {
				DiscordPlugin.dc = dc; //Set to gateway client
				dc.on(ReadyEvent.class) // Listen for ReadyEvent(s)
					.map(event -> event.getGuilds().size()) // Get how many guilds the bot is in
					.flatMap(size -> dc
						.on(GuildCreateEvent.class) // Listen for GuildCreateEvent(s)
						.take(size) // Take only the first `size` GuildCreateEvent(s) to be received
						.collectList()).subscribe(this::handleReady); // Take all received GuildCreateEvents and make it a List
			}); /* All guilds have been received, client is fully connected */
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to enable the Discord plugin!", e, this);
			getLogger().severe("You may be able to reset the plugin using /discord reset");
		}
	}

	public static Guild mainServer;

	private void handleReady(List<GuildCreateEvent> event) {
		try {
			if (mainServer != null) { //This is not the first ready event
				getLogger().info("Ready event already handled"); //TODO: It should probably handle disconnections
				dc.updatePresence(Presence.online(Activity.playing("Minecraft"))).subscribe(); //Update from the initial presence
				return;
			}
			mainServer = mainServer().get().orElse(null); //Shouldn't change afterwards
			if (mainServer == null) {
				if (event.size() == 0) {
					getLogger().severe("Main server not found! Invite the bot and do /discord reset");
					dc.getApplicationInfo().subscribe(info ->
						getLogger().severe("Click here: https://discordapp.com/oauth2/authorize?client_id=" + info.getId().asString() + "&scope=bot&permissions=268509264"));
					saveConfig(); //Put default there
					return; //We should have all guilds by now, no need to retry
				}
				mainServer = event.get(0).getGuild();
				getLogger().warning("Main server set to first one: " + mainServer.getName());
				mainServer().set(Optional.of(mainServer)); //Save in config
			}
			SafeMode = false;
			DPUtils.disableIfConfigErrorRes(null, commandChannel(), DPUtils.getMessageChannel(commandChannel()));
			//Won't disable, just prints the warning here

			Component.registerComponent(this, new GeneralEventBroadcasterModule());
			Component.registerComponent(this, new MinecraftChatModule());
			Component.registerComponent(this, new ExceptionListenerModule());
			Component.registerComponent(this, new GameRoleModule()); //Needs the mainServer to be set
			Component.registerComponent(this, new AnnouncerModule());
			Component.registerComponent(this, new FunModule());
			new ChromaBot(this).updatePlayerList(); //Initialize ChromaBot - The MCChatModule is tested to be enabled

			getManager().registerCommand(new VersionCommand());
			getManager().registerCommand(new UserinfoCommand());
			getManager().registerCommand(new HelpCommand());
			getManager().registerCommand(new DebugCommand());
			getManager().registerCommand(new ConnectCommand());
			if (DiscordMCCommand.resetting) //These will only execute if the chat is enabled
				ChromaBot.getInstance().sendMessageCustomAsWell(chan -> chan.flatMap(ch -> ch.createEmbed(ecs -> ecs.setColor(Color.CYAN)
					.setTitle("Discord plugin restarted - chat connected."))), ChannelconBroadcast.RESTART); //Really important to note the chat, hmm
			else if (getConfig().getBoolean("serverup", false)) {
				ChromaBot.getInstance().sendMessageCustomAsWell(chan -> chan.flatMap(ch -> ch.createEmbed(ecs -> ecs.setColor(Color.YELLOW)
					.setTitle("Server recovered from a crash - chat connected."))), ChannelconBroadcast.RESTART);
				val thr = new Throwable(
					"The server shut down unexpectedly. See the log of the previous run for more details.");
				thr.setStackTrace(new StackTraceElement[0]);
				TBMCCoreAPI.SendException("The server crashed!", thr, this);
			} else
				ChromaBot.getInstance().sendMessageCustomAsWell(chan -> chan.flatMap(ch -> ch.createEmbed(ecs -> ecs.setColor(Color.GREEN)
					.setTitle("Server started - chat connected."))), ChannelconBroadcast.RESTART);

			DiscordMCCommand.resetting = false; //This is the last event handling this flag

			getConfig().set("serverup", true);
			saveConfig();
			TBMCCoreAPI.SendUnsentExceptions();
			TBMCCoreAPI.SendUnsentDebugMessages();

			CommonListeners.register(dc.getEventDispatcher());
			TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), this);
			TBMCCoreAPI.RegisterUserClass(DiscordPlayer.class);
			ChromaGamerBase.addConverter(sender -> Optional.ofNullable(sender instanceof DiscordSenderBase
				? ((DiscordSenderBase) sender).getChromaUser() : null));

			IHaveConfig.pregenConfig(this, null);
			if (!TBMCCoreAPI.IsTestServer()) {
				dc.updatePresence(Presence.online(Activity.playing("Minecraft"))).subscribe();
			} else {
				dc.updatePresence(Presence.online(Activity.playing("testing"))).subscribe();
			}
			getLogger().info("Loaded!");
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occurred while enabling DiscordPlugin!", e, this);
		}
	}

	/**
	 * Always true, except when running "stop" from console
	 */
	public static boolean Restart;

	@Override
	public void pluginPreDisable() {
		if (ChromaBot.getInstance() == null) return; //Failed to load
		Timings timings = new Timings();
		timings.printElapsed("Disable start");
		MCChatUtils.forCustomAndAllMCChat(chan -> chan.flatMap(ch -> ch.createEmbed(ecs -> {
			timings.printElapsed("Sending message to " + ch.getMention());
			if (DiscordMCCommand.resetting)
				ecs.setColor(Color.ORANGE).setTitle("Discord plugin restarting");
			else
				ecs.setColor(Restart ? Color.ORANGE : Color.RED)
					.setTitle(Restart ? "Server restarting" : "Server stopping")
					.setDescription(
						Bukkit.getOnlinePlayers().size() > 0
							? (DPUtils
							.sanitizeString(Bukkit.getOnlinePlayers().stream()
								.map(Player::getDisplayName).collect(Collectors.joining(", ")))
							+ (Bukkit.getOnlinePlayers().size() == 1 ? " was " : " were ")
							+ "thrown out") //TODO: Make configurable
							: ""); //If 'restart' is disabled then this isn't shown even if joinleave is enabled
		})).subscribe(), ChannelconBroadcast.RESTART, false);
		timings.printElapsed("Updating player list");
		ChromaBot.getInstance().updatePlayerList();
		timings.printElapsed("Done");
	}

	@Override
	public void pluginDisable() {
		Timings timings = new Timings();
		timings.printElapsed("Actual disable start (logout)");
		timings.printElapsed("Config setup");
		getConfig().set("serverup", false);
		if (ChromaBot.getInstance() == null) return; //Failed to load

		saveConfig();
		try {
			SafeMode = true; // Stop interacting with Discord
			ChromaBot.delete();
			//timings.printElapsed("Updating presence...");
			//dc.updatePresence(Presence.idle(Activity.playing("logging out"))).block(); //No longer using the same account for testing
			timings.printElapsed("Logging out...");
			dc.logout().block();
			mainServer = null; //Allow ReadyEvent again
			//Configs are emptied so channels and servers are fetched again
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e, this);
		}
	}

	public static final ReactionEmoji DELIVERED_REACTION = ReactionEmoji.unicode("✅");
}
