package buttondevteam.discordplugin;

import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.discordplugin.commands.Command2DC;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import buttondevteam.discordplugin.commands.UserinfoCommand;
import buttondevteam.discordplugin.commands.VersionCommand;
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule;
import buttondevteam.discordplugin.fun.FunModule;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.discordplugin.listeners.MCListener;
import buttondevteam.discordplugin.mcchat.ChannelconCommand;
import buttondevteam.discordplugin.mcchat.MCChatPrivate;
import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.mccommands.DiscordMCCommandBase;
import buttondevteam.discordplugin.mccommands.ResetMCCommand;
import buttondevteam.discordplugin.role.GameRoleModule;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.ButtonPlugin;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.ChromaGamerBase;
import com.google.common.io.Files;
import lombok.Getter;
import lombok.val;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.scheduler.BukkitTask;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.ReadyEvent;
import sx.blah.discord.handle.impl.obj.ReactionEmoji;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.RequestBuffer;

import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class DiscordPlugin extends ButtonPlugin implements IListener<ReadyEvent> {
    public static IDiscordClient dc;
    public static DiscordPlugin plugin;
    public static boolean SafeMode = true;
	@Getter
	private Command2DC manager;

	public ConfigData<Character> Prefix() {
	    return getIConfig().getData("prefix", '/', str -> ((String) str).charAt(0), Object::toString);
    }

    public static char getPrefix() {
        if (plugin == null) return '/';
        return plugin.Prefix().get();
    }

	public ConfigData<IGuild> MainServer() {
		return getIConfig().getDataPrimDef("mainServer", 219529124321034241L, id -> dc.getGuildByID((long) id), IIDLinkedObject::getLongID);
	}

	public ConfigData<IChannel> CommandChannel() {
		return DPUtils.channelData(getIConfig(), "commandChannel", 239519012529111040L);
	}

	public ConfigData<IRole> ModRole() {
		return DPUtils.roleData(getIConfig(), "modRole", "Moderator");
	}

    @Override
    public void pluginEnable() {
        try {
	        getLogger().info("Initializing...");
            plugin = this;
	        manager = new Command2DC();
            ClientBuilder cb = new ClientBuilder();
            cb.withToken(Files.readFirstLine(new File("TBMC", "Token.txt"), StandardCharsets.UTF_8));
            dc = cb.login();
            dc.getDispatcher().registerListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

    public static IGuild mainServer;
    public static IGuild devServer;

    private static volatile BukkitTask task;
    private static volatile boolean sent = false;

    @Override
    public void handle(ReadyEvent event) {
        try {
            dc.changePresence(StatusType.DND, ActivityType.PLAYING, "booting");
            task = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
                if (mainServer == null || devServer == null) {
                    mainServer = event.getClient().getGuildByID(125813020357165056L);
                    devServer = event.getClient().getGuildByID(219529124321034241L);
                }
                if (mainServer == null || devServer == null)
                    return; // Retry
                if (!TBMCCoreAPI.IsTestServer()) { //Don't change conditions here, see mainServer=devServer=null in onDisable()
                    dc.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "Chromacraft");
                } else {
                    dc.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "testing");
                }
                SafeMode = false;
                if (task != null)
                    task.cancel();
                if (!sent) {
	                Component.registerComponent(this, new GeneralEventBroadcasterModule());
	                Component.registerComponent(this, new MinecraftChatModule());
	                Component.registerComponent(this, new ExceptionListenerModule());
	                Component.registerComponent(this, new GameRoleModule()); //Needs the mainServer to be set
	                Component.registerComponent(this, new AnnouncerModule());
					Component.registerComponent(this, new FunModule());
	                new ChromaBot(this).updatePlayerList(); //Initialize ChromaBot - The MCCHatModule is tested to be enabled

                    DiscordCommandBase.registerCommands();
	                getManager().registerCommand(new VersionCommand());
	                getManager().registerCommand(new UserinfoCommand());
	                getManager().registerCommand(new ChannelconCommand());
	                if (ResetMCCommand.resetting) //These will only execute if the chat is enabled
                        ChromaBot.getInstance().sendMessageCustomAsWell("", new EmbedBuilder().withColor(Color.CYAN)
                                .withTitle("Discord plugin restarted - chat connected.").build(), ChannelconBroadcast.RESTART); //Really important to note the chat, hmm
                    else if (getConfig().getBoolean("serverup", false)) {
                        ChromaBot.getInstance().sendMessageCustomAsWell("", new EmbedBuilder().withColor(Color.YELLOW)
                                .withTitle("Server recovered from a crash - chat connected.").build(), ChannelconBroadcast.RESTART);
                        val thr = new Throwable(
                                "The server shut down unexpectedly. See the log of the previous run for more details.");
                        thr.setStackTrace(new StackTraceElement[0]);
                        TBMCCoreAPI.SendException("The server crashed!", thr);
                    } else
                        ChromaBot.getInstance().sendMessageCustomAsWell("", new EmbedBuilder().withColor(Color.GREEN)
                                .withTitle("Server started - chat connected.").build(), ChannelconBroadcast.RESTART);

                    ResetMCCommand.resetting = false; //This is the last event handling this flag

                    getConfig().set("serverup", true);
                    saveConfig();
                    sent = true;
                    if (TBMCCoreAPI.IsTestServer() && !dc.getOurUser().getName().toLowerCase().contains("test")) {
                        TBMCCoreAPI.SendException(
                                "Won't load because we're in testing mode and not using a separate account.",
                                new Exception(
	                                "The plugin refuses to load until you change the token to a testing account. (The account needs to have \"test\" in it's name.)"));
                        Bukkit.getPluginManager().disablePlugin(this);
                    }
                    TBMCCoreAPI.SendUnsentExceptions();
                    TBMCCoreAPI.SendUnsentDebugMessages();
                }
            }, 0, 10);
            for (IListener<?> listener : CommonListeners.getListeners())
                dc.getDispatcher().registerListener(listener);
            TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), this);
            TBMCChatAPI.AddCommands(this, DiscordMCCommandBase.class);
            TBMCCoreAPI.RegisterUserClass(DiscordPlayer.class);
            ChromaGamerBase.addConverter(sender -> Optional.ofNullable(sender instanceof DiscordSenderBase
                    ? ((DiscordSenderBase) sender).getChromaUser() : null));
            setupProviders();
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
		EmbedObject embed;
		if (ResetMCCommand.resetting)
			embed = new EmbedBuilder().withColor(Color.ORANGE).withTitle("Discord plugin restarting").build();
		else
			embed = new EmbedBuilder().withColor(Restart ? Color.ORANGE : Color.RED)
				.withTitle(Restart ? "Server restarting" : "Server stopping")
				.withDescription(
					Bukkit.getOnlinePlayers().size() > 0
						? (DPUtils
						.sanitizeString(Bukkit.getOnlinePlayers().stream()
							.map(Player::getDisplayName).collect(Collectors.joining(", ")))
						+ (Bukkit.getOnlinePlayers().size() == 1 ? " was " : " were ")
						+ "kicked the hell out.") //TODO: Make configurable
						: "") //If 'restart' is disabled then this isn't shown even if joinleave is enabled
				.build();
		MCChatUtils.forCustomAndAllMCChat(ch -> {
			try {
				DiscordPlugin.sendMessageToChannelWait(ch, "",
					embed, 5, TimeUnit.SECONDS);
			} catch (TimeoutException | InterruptedException e) {
				e.printStackTrace();
			}
		}, ChannelconBroadcast.RESTART, false);
		ChromaBot.getInstance().updatePlayerList();
	}

	@Override
	public void pluginDisable() {
		MCChatPrivate.logoutAll();
		getConfig().set("serverup", false);

		saveConfig();
        try {
            SafeMode = true; // Stop interacting with Discord
            ChromaBot.delete();
            dc.changePresence(StatusType.IDLE, ActivityType.PLAYING, "Chromacraft"); //No longer using the same account for testing
            dc.logout();
            mainServer = devServer = null; //Fetch servers and channels again
            sent = false;
        } catch (Exception e) {
            TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e);
        }
    }

    public static final ReactionEmoji DELIVERED_REACTION = ReactionEmoji.of("✅");

    public static void sendMessageToChannel(IChannel channel, String message) {
        sendMessageToChannel(channel, message, null);
    }

    public static void sendMessageToChannel(IChannel channel, String message, EmbedObject embed) {
        try {
            sendMessageToChannel(channel, message, embed, false);
        } catch (TimeoutException | InterruptedException e) {
            e.printStackTrace(); //Shouldn't happen, as we're not waiting on the result
        }
    }

    public static IMessage sendMessageToChannelWait(IChannel channel, String message) throws TimeoutException, InterruptedException {
        return sendMessageToChannelWait(channel, message, null);
    }

    public static IMessage sendMessageToChannelWait(IChannel channel, String message, EmbedObject embed) throws TimeoutException, InterruptedException {
        return sendMessageToChannel(channel, message, embed, true);
    }

    public static IMessage sendMessageToChannelWait(IChannel channel, String message, EmbedObject embed, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        return sendMessageToChannel(channel, message, embed, true, timeout, unit);
    }

    private static IMessage sendMessageToChannel(IChannel channel, String message, EmbedObject embed, boolean wait) throws TimeoutException, InterruptedException {
        return sendMessageToChannel(channel, message, embed, wait, -1, null);
    }

    private static IMessage sendMessageToChannel(IChannel channel, String message, EmbedObject embed, boolean wait, long timeout, TimeUnit unit) throws TimeoutException, InterruptedException {
        if (message.length() > 1980) {
            message = message.substring(0, 1980);
	        DPUtils.getLogger()
                    .warning("Message was too long to send to discord and got truncated. In " + channel.getName());
        }
        try {
            MCChatUtils.resetLastMessage(channel); // If this is a chat message, it'll be set again
            final String content = message;
            RequestBuffer.IRequest<IMessage> r = () -> embed == null ? channel.sendMessage(content)
                    : channel.sendMessage(content, embed, false);
            if (wait) {
                if (unit != null)
                    return DPUtils.perform(r, timeout, unit);
                else
                    return DPUtils.perform(r);
            } else {
                if (unit != null)
                    plugin.getLogger().warning("Tried to set timeout for non-waiting call.");
                else
                    DPUtils.performNoWait(r);
                return null;
            }
        } catch (TimeoutException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
	        DPUtils.getLogger().warning(
                    "Failed to deliver message to Discord! Channel: " + channel.getName() + " Message: " + message);
            throw new RuntimeException(e);
        }
    }

    public static Permission perms;

    public boolean setupProviders() {
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
