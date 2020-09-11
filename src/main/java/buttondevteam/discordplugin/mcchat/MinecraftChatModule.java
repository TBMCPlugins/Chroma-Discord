package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.MainPlugin;
import buttondevteam.core.component.channel.Channel;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.playerfaker.ServerWatcher;
import buttondevteam.discordplugin.playerfaker.perm.LPInjector;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import com.google.common.collect.Lists;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.util.Snowflake;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Provides Minecraft chat connection to Discord. Commands may be used either in a public chat (limited) or in a DM.
 */
public class MinecraftChatModule extends Component<DiscordPlugin> {
	private @Getter MCChatListener listener;
	private ServerWatcher serverWatcher;

	/**
	 * A list of commands that can be used in public chats - Warning: Some plugins will treat players as OPs, always test before allowing a command!
	 */
	public ConfigData<ArrayList<String>> whitelistedCommands() {
		return getConfig().getData("whitelistedCommands", () -> Lists.newArrayList("list", "u", "shrug", "tableflip", "unflip", "mwiki",
			"yeehaw", "lenny", "rp", "plugins"));
	}

	/**
	 * The channel to use as the public Minecraft chat - everything public gets broadcasted here
	 */
	public ReadOnlyConfigData<Snowflake> chatChannel() {
		return DPUtils.snowflakeData(getConfig(), "chatChannel", 0L);
	}

	public Mono<MessageChannel> chatChannelMono() {
		return DPUtils.getMessageChannel(chatChannel().getPath(), chatChannel().get());
	}

	/**
	 * The channel where the plugin can log when it mutes a player on Discord because of a Minecraft mute
	 */
	public ReadOnlyConfigData<Mono<MessageChannel>> modlogChannel() {
		return DPUtils.channelData(getConfig(), "modlogChannel");
	}

	/**
	 * The plugins to exclude from fake player events used for the 'mcchat' command - some plugins may crash, add them here
	 */
	public ConfigData<String[]> excludedPlugins() {
		return getConfig().getData("excludedPlugins", new String[]{"ProtocolLib", "LibsDisguises", "JourneyMapServer"});
	}

	/**
	 * If this setting is on then players logged in through the 'mcchat' command will be able to teleport using plugin commands.
	 * They can then use commands like /tpahere to teleport others to that place.<br />
	 * If this is off, then teleporting will have no effect.
	 */
	public ConfigData<Boolean> allowFakePlayerTeleports() {
		return getConfig().getData("allowFakePlayerTeleports", false);
	}

	/**
	 * If this is on, each chat channel will have a player list in their description.
	 * It only gets added if there's no description yet or there are (at least) two lines of "----" following each other.
	 * Note that it will replace <b>everything</b> above the first and below the last "----" but it will only detect exactly four dashes.
	 * So if you want to use dashes for something else in the description, make sure it's either less or more dashes in one line.
	 */
	public ConfigData<Boolean> showPlayerListOnDC() {
		return getConfig().getData("showPlayerListOnDC", true);
	}

	/**
	 * This setting controls whether custom chat connections can be <i>created</i> (existing connections will always work).
	 * Custom chat connections can be created using the channelcon command and they allow players to display town chat in a Discord channel for example.
	 * See the channelcon command for more details.
	 */
	public ConfigData<Boolean> allowCustomChat() {
		return getConfig().getData("allowCustomChat", true);
	}

	/**
	 * This setting allows you to control if players can DM the bot to log on the server from Discord.
	 * This allows them to both chat and perform any command they can in-game.
	 */
	public ConfigData<Boolean> allowPrivateChat() {
		return getConfig().getData("allowPrivateChat", true);
	}

	/**
	 * If set, message authors appearing on Discord will link to this URL. A 'type' and 'id' parameter will be added with the user's platform (Discord, Minecraft, ...) and ID.
	 */
	public ConfigData<String> profileURL() {
		return getConfig().getData("profileURL", "");
	}

	/**
	 * Enables support for running vanilla commands through Discord, if you ever need it.
	 */
	public ConfigData<Boolean> enableVanillaCommands() {
		return getConfig().getData("enableVanillaCommands", true);
	}

	/**
	 * Whether players logged on from Discord should be recognised by other plugins. Some plugins might break if it's turned off.
	 * But it's really hacky.
	 */
	public final ConfigData<Boolean> addFakePlayersToBukkit = getConfig().getData("addFakePlayersToBukkit", true);

	@Override
	protected void enable() {
		if (DPUtils.disableIfConfigErrorRes(this, chatChannel(), chatChannelMono()))
			return;
		/*clientID = DiscordPlugin.dc.getApplicationInfo().blockOptional().map(info->info.getId().asString())
			.orElse("Unknown"); //Need to block because otherwise it may not be set in time*/
		listener = new MCChatListener(this);
		TBMCCoreAPI.RegisterEventsForExceptions(listener, getPlugin());
		TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(this), getPlugin());//These get undone if restarting/resetting - it will ignore events if disabled
		getPlugin().getManager().registerCommand(new MCChatCommand(this));
		getPlugin().getManager().registerCommand(new ChannelconCommand(this));

		val chcons = getConfig().getConfig().getConfigurationSection("chcons");
		if (chcons == null) //Fallback to old place
			getConfig().getConfig().getRoot().getConfigurationSection("chcons");
		if (chcons != null) {
			val chconkeys = chcons.getKeys(false);
			for (val chconkey : chconkeys) {
				val chcon = chcons.getConfigurationSection(chconkey);
				val mcch = Channel.getChannels().filter(ch -> ch.ID.equals(chcon.getString("mcchid"))).findAny();
				val ch = DiscordPlugin.dc.getChannelById(Snowflake.of(chcon.getLong("chid"))).block();
				val did = chcon.getLong("did");
				val user = DiscordPlugin.dc.getUserById(Snowflake.of(did)).block();
				val groupid = chcon.getString("groupid");
				val toggles = chcon.getInt("toggles");
				val brtoggles = chcon.getStringList("brtoggles");
				if (!mcch.isPresent() || ch == null || user == null || groupid == null)
					continue;
				Bukkit.getScheduler().runTask(getPlugin(), () -> { //<-- Needed because of occasional ConcurrentModificationExceptions when creating the player (PermissibleBase)
					val dcp = DiscordConnectedPlayer.create(user, (MessageChannel) ch, UUID.fromString(chcon.getString("mcuid")), chcon.getString("mcname"), this);
					MCChatCustom.addCustomChat((MessageChannel) ch, groupid, mcch.get(), user, dcp, toggles, brtoggles.stream().map(TBMCSystemChatEvent.BroadcastTarget::get).filter(Objects::nonNull).collect(Collectors.toSet()));
				});
			}
		}

		try {
			new LPInjector(MainPlugin.Instance);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to init LuckPerms injector", e);
		} catch (NoClassDefFoundError e) {
			log("No LuckPerms, not injecting");
			//e.printStackTrace();
		}

		if (addFakePlayersToBukkit.get()) {
			try {
				serverWatcher = new ServerWatcher();
				serverWatcher.enableDisable(true);
			} catch (Exception e) {
				TBMCCoreAPI.SendException("Failed to hack the server (object)!", e);
			}
		}
	}

	@Override
	protected void disable() {
		try { //If it's not enabled it won't do anything
			if (serverWatcher != null)
				serverWatcher.enableDisable(false);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to restore the server object!", e);
		}
		val chcons = MCChatCustom.getCustomChats();
		val chconsc = getConfig().getConfig().createSection("chcons");
		for (val chcon : chcons) {
			val chconc = chconsc.createSection(chcon.channel.getId().asString());
			chconc.set("mcchid", chcon.mcchannel.ID);
			chconc.set("chid", chcon.channel.getId().asLong());
			chconc.set("did", chcon.user.getId().asLong());
			chconc.set("mcuid", chcon.dcp.getUniqueId().toString());
			chconc.set("mcname", chcon.dcp.getName());
			chconc.set("groupid", chcon.groupID);
			chconc.set("toggles", chcon.toggles);
			chconc.set("brtoggles", chcon.brtoggles.stream().map(TBMCSystemChatEvent.BroadcastTarget::getName).collect(Collectors.toList()));
		}
		MCChatListener.stop(true);
	}
}
