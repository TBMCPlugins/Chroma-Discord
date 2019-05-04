package buttondevteam.discordplugin;

import buttondevteam.discordplugin.announcer.AnnouncerModule;
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.discordplugin.commands.*;
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule;
import buttondevteam.discordplugin.fun.FunModule;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.discordplugin.listeners.MCListener;
import buttondevteam.discordplugin.mcchat.MCChatPrivate;
import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.mccommands.DiscordMCCommand;
import buttondevteam.discordplugin.role.GameRoleModule;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.ButtonPlugin;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.IHaveConfig;
import buttondevteam.lib.player.ChromaGamerBase;
import com.google.common.io.Files;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.lifecycle.ReadyEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Role;
import discord4j.core.object.presence.Activity;
import discord4j.core.object.presence.Presence;
import discord4j.core.object.reaction.ReactionEmoji;
import discord4j.core.object.util.Snowflake;
import lombok.Getter;
import lombok.val;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@ButtonPlugin.ConfigOpts(disableConfigGen = true)
public class DiscordPlugin extends ButtonPlugin {
	public static DiscordClient dc;
	public static DiscordPlugin plugin;
	public static boolean SafeMode = true;
	@Getter
	private Command2DC manager;

	private ConfigData<Character> Prefix() {
		return getIConfig().getData("prefix", '/', str -> ((String) str).charAt(0), Object::toString);
	}

	public static char getPrefix() {
		if (plugin == null) return '/';
		return plugin.Prefix().get();
	}

	private ConfigData<Guild> MainServer() {
		return getIConfig().getDataPrimDef("mainServer", 219529124321034241L, id -> dc.getGuildById(Snowflake.of((long) id)).block(), g -> g.getId().asLong());
	}

	public ConfigData<Snowflake> CommandChannel() {
		return DPUtils.snowflakeData(getIConfig(), "commandChannel", 239519012529111040L);
	}

	public ConfigData<Mono<Role>> ModRole() {
		return DPUtils.roleData(getIConfig(), "modRole", "Moderator");
	}

	/**
	 * The invite link to show by /discord invite. If empty, it defaults to the first invite if the bot has access.
	 */
	public ConfigData<String> InviteLink() {
		return getIConfig().getData("inviteLink", "");
	}

	@Override
	public void pluginEnable() {
		try {
			getLogger().info("Initializing...");
			plugin = this;
			manager = new Command2DC();
			String token;
			File tokenFile = new File("TBMC", "Token.txt");
			if (tokenFile.exists()) //Legacy support
				//noinspection UnstableApiUsage
				token = Files.readFirstLine(tokenFile, StandardCharsets.UTF_8);
			else {
				File privateFile = new File(getDataFolder(), "private.yml");
				val conf = YamlConfiguration.loadConfiguration(privateFile);
				token = conf.getString("token");
				if (token == null) {
					conf.set("token", "Token goes here");
					conf.save(privateFile);

					getLogger().severe("Token not found! Set it in private.yml");
					Bukkit.getPluginManager().disablePlugin(this);
					return;
				}
			}
			val cb = new DiscordClientBuilder(token);
			cb.setInitialPresence(Presence.doNotDisturb(Activity.playing("booting")));
			dc = cb.build();
			dc.getEventDispatcher().on(ReadyEvent.class) // Listen for ReadyEvent(s)
				.map(event -> event.getGuilds().size()) // Get how many guilds the bot is in
				.flatMap(size -> dc.getEventDispatcher()
					.on(GuildCreateEvent.class) // Listen for GuildCreateEvent(s)
					.take(size) // Take only the first `size` GuildCreateEvent(s) to be received
					.collectList()) // Take all received GuildCreateEvents and make it a List
				.subscribe(this::handleReady); /* All guilds have been received, client is fully connected */
			dc.login().subscribe();
		} catch (Exception e) {
			e.printStackTrace();
			Bukkit.getPluginManager().disablePlugin(this);
		}
	}

	public static Guild mainServer;

	private void handleReady(List<GuildCreateEvent> event) {
		try {
			mainServer = MainServer().get(); //Shouldn't change afterwards
			if (mainServer == null) {
				if (event.size() == 0) {
					getLogger().severe("Main server not found! Invite the bot and do /discord reset");
					saveConfig(); //Put default there
					return; //We should have all guilds by now, no need to retry
				}
				mainServer = event.get(0).getGuild();
				getLogger().warning("Main server set to first one: " + mainServer.getName());
				MainServer().set(mainServer); //Save in config
			}
			if (!TBMCCoreAPI.IsTestServer()) { //Don't change conditions here, see mainServer=devServer=null in onDisable()
				dc.updatePresence(Presence.online(Activity.playing("Minecraft"))).subscribe();
			} else {
				dc.updatePresence(Presence.online(Activity.playing("testing"))).subscribe();
			}
			SafeMode = false;
			DPUtils.disableIfConfigError(null, CommandChannel(), ModRole()); //Won't disable, just prints the warning here

			Component.registerComponent(this, new GeneralEventBroadcasterModule());
			Component.registerComponent(this, new MinecraftChatModule());
			Component.registerComponent(this, new ExceptionListenerModule());
			Component.registerComponent(this, new GameRoleModule()); //Needs the mainServer to be set
			Component.registerComponent(this, new AnnouncerModule());
			Component.registerComponent(this, new FunModule());
			new ChromaBot(this).updatePlayerList(); //Initialize ChromaBot - The MCCHatModule is tested to be enabled

			getManager().registerCommand(new VersionCommand());
			getManager().registerCommand(new UserinfoCommand());
			getManager().registerCommand(new HelpCommand());
			getManager().registerCommand(new DebugCommand());
			getManager().registerCommand(new ConnectCommand());
			if (DiscordMCCommand.resetting) //These will only execute if the chat is enabled
				ChromaBot.getInstance().sendMessageCustomAsWell(ch -> ch.createEmbed(ecs -> ecs.setColor(Color.CYAN)
					.setTitle("Discord plugin restarted - chat connected.")), ChannelconBroadcast.RESTART); //Really important to note the chat, hmm
			else if (getConfig().getBoolean("serverup", false)) {
				ChromaBot.getInstance().sendMessageCustomAsWell(ch -> ch.createEmbed(ecs -> ecs.setColor(Color.YELLOW)
					.setTitle("Server recovered from a crash - chat connected.")), ChannelconBroadcast.RESTART);
				val thr = new Throwable(
					"The server shut down unexpectedly. See the log of the previous run for more details.");
				thr.setStackTrace(new StackTraceElement[0]);
				TBMCCoreAPI.SendException("The server crashed!", thr);
			} else
				ChromaBot.getInstance().sendMessageCustomAsWell(ch -> ch.createEmbed(ecs -> ecs.setColor(Color.GREEN)
					.setTitle("Server started - chat connected.")), ChannelconBroadcast.RESTART);

			DiscordMCCommand.resetting = false; //This is the last event handling this flag

			getConfig().set("serverup", true);
			saveConfig();
			if (TBMCCoreAPI.IsTestServer() && !Objects.requireNonNull(dc.getSelf().block()).getUsername().toLowerCase().contains("test")) {
				TBMCCoreAPI.SendException(
					"Won't load because we're in testing mode and not using a separate account.",
					new Exception(
						"The plugin refuses to load until you change the token to a testing account. (The account needs to have \"test\" in it's name.)"));
				Bukkit.getPluginManager().disablePlugin(this);
			}
			TBMCCoreAPI.SendUnsentExceptions();
			TBMCCoreAPI.SendUnsentDebugMessages();

			CommonListeners.register(dc.getEventDispatcher());
			TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), this);
			getCommand2MC().registerCommand(new DiscordMCCommand());
			TBMCCoreAPI.RegisterUserClass(DiscordPlayer.class);
			ChromaGamerBase.addConverter(sender -> Optional.ofNullable(sender instanceof DiscordSenderBase
				? ((DiscordSenderBase) sender).getChromaUser() : null));
			setupProviders();

			IHaveConfig.pregenConfig(this, null);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while enabling DiscordPlugin!", e);
		}
	}

	/**
	 * Always true, except when running "stop" from console
	 */
	public static boolean Restart;

	@Override
	public void pluginPreDisable() {
		if (ChromaBot.getInstance() == null) return; //Failed to load
		System.out.println("Disable start");
		MCChatUtils.forCustomAndAllMCChat(ch -> ch.createEmbed(ecs -> {
			System.out.println("Sending message to " + ch.getMention());
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
							+ "kicked the hell out.") //TODO: Make configurable
							: ""); //If 'restart' is disabled then this isn't shown even if joinleave is enabled
		}).block(), ChannelconBroadcast.RESTART, false);
		System.out.println("Updating player list");
		ChromaBot.getInstance().updatePlayerList();
		System.out.println("Done");
	}

	@Override
	public void pluginDisable() {
		System.out.println("Actual disable start (logout)");
		MCChatPrivate.logoutAll();
		System.out.println("Config setup");
		getConfig().set("serverup", false);
		if (ChromaBot.getInstance() == null) return; //Failed to load

		saveConfig();
		try {
			SafeMode = true; // Stop interacting with Discord
			ChromaBot.delete();
			System.out.println("Updating presence...");
			dc.updatePresence(Presence.idle(Activity.playing("Chromacraft"))).block(); //No longer using the same account for testing
			System.out.println("Logging out...");
			dc.logout().block();
			//Configs are emptied so channels and servers are fetched again
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e);
		}
	}

	public static final ReactionEmoji DELIVERED_REACTION = ReactionEmoji.unicode("✅");

	public static Permission perms;

	private boolean setupProviders() {
		try {
			Class.forName("net.milkbowl.vault.permission.Permission");
			Class.forName("net.milkbowl.vault.chat.Chat");
		} catch (ClassNotFoundException e) {
			return false;
		}

		RegisteredServiceProvider<Permission> permsProvider = Bukkit.getServer().getServicesManager()
			.getRegistration(Permission.class);
		perms = permsProvider.getProvider();
		return perms != null;
	}
}
