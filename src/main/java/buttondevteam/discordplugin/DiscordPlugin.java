package buttondevteam.discordplugin;

import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule;
import buttondevteam.discordplugin.listeners.CommonListeners;
import buttondevteam.discordplugin.listeners.MCListener;
import buttondevteam.discordplugin.mcchat.*;
import buttondevteam.discordplugin.mccommands.DiscordMCCommandBase;
import buttondevteam.discordplugin.mccommands.ResetMCCommand;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.ButtonPlugin;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.ChromaGamerBase;
import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

public class DiscordPlugin extends ButtonPlugin implements IListener<ReadyEvent> {
    private static final String SubredditURL = "https://www.reddit.com/r/ChromaGamers";
    private static boolean stop = false;
    public static IDiscordClient dc;
    public static DiscordPlugin plugin;
    public static boolean SafeMode = true;
    public static List<String> GameRoles;

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

    @Override
    public void pluginEnable() {
        stop = false; //If not the first time
        try {
            Bukkit.getLogger().info("Initializing DiscordPlugin...");
            plugin = this;
            lastannouncementtime = getConfig().getLong("lastannouncementtime");
            lastseentime = getConfig().getLong("lastseentime");
            ClientBuilder cb = new ClientBuilder();
            cb.withToken(Files.readFirstLine(new File("TBMC", "Token.txt"), StandardCharsets.UTF_8));
            dc = cb.login();
            dc.getDispatcher().registerListener(this);
        } catch (Exception e) {
            e.printStackTrace();
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }

	public static IChannel botchannel; //Can be removed
    public static IChannel annchannel;
    public static IChannel genchannel;
    public static IChannel chatchannel;
    public static IChannel botroomchannel;
    public static IChannel modlogchannel;
    /**
     * Don't send messages, just receive, the same channel is used when testing
     */
    public static IChannel officechannel;
    public static IChannel updatechannel;
    public static IChannel devofficechannel;
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
                    botchannel = mainServer.getChannelByID(209720707188260864L); // bot
                    annchannel = mainServer.getChannelByID(126795071927353344L); // announcements
                    genchannel = mainServer.getChannelByID(125813020357165056L); // general
                    chatchannel = mainServer.getChannelByID(249663564057411596L); // minecraft_chat
                    botroomchannel = devServer.getChannelByID(239519012529111040L); // bot-room
                    officechannel = devServer.getChannelByID(219626707458457603L); // developers-office
                    updatechannel = devServer.getChannelByID(233724163519414272L); // server-updates
                    devofficechannel = officechannel; // developers-office
                    modlogchannel = mainServer.getChannelByID(283840717275791360L); // modlog
                    dc.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "Chromacraft");
                } else {
                    botchannel = devServer.getChannelByID(239519012529111040L); // bot-room
                    annchannel = botchannel; // bot-room
                    genchannel = botchannel; // bot-room
                    botroomchannel = botchannel;// bot-room
                    chatchannel = botchannel;// bot-room
                    officechannel = devServer.getChannelByID(219626707458457603L); // developers-office
                    updatechannel = botchannel;
                    devofficechannel = botchannel;// bot-room
                    modlogchannel = botchannel; // bot-room
                    dc.changePresence(StatusType.ONLINE, ActivityType.PLAYING, "testing");
                }
                if (botchannel == null || annchannel == null || genchannel == null || botroomchannel == null
                        || chatchannel == null || officechannel == null || updatechannel == null)
                    return; // Retry
                SafeMode = false;
                if (task != null)
                    task.cancel();
                if (!sent) {
                    new ChromaBot(this).updatePlayerList();
                    GameRoles = mainServer.getRoles().stream().filter(this::isGameRole).map(IRole::getName).collect(Collectors.toList());

                    val chcons = getConfig().getConfigurationSection("chcons");
                    if (chcons != null) {
                        val chconkeys = chcons.getKeys(false);
                        for (val chconkey : chconkeys) {
                            val chcon = chcons.getConfigurationSection(chconkey);
                            val mcch = Channel.getChannels().stream().filter(ch -> ch.ID.equals(chcon.getString("mcchid"))).findAny();
                            val ch = dc.getChannelByID(chcon.getLong("chid"));
                            val did = chcon.getLong("did");
                            val user = dc.fetchUser(did);
                            val groupid = chcon.getString("groupid");
                            val toggles = chcon.getInt("toggles");
                            if (!mcch.isPresent() || ch == null || user == null || groupid == null)
                                continue;
	                        Bukkit.getScheduler().runTask(this, () -> { //<-- Needed because of occasional ConcurrentModificationExceptions when creating the player (PermissibleBase)
		                        val dcp = new DiscordConnectedPlayer(user, ch, UUID.fromString(chcon.getString("mcuid")), chcon.getString("mcname"));
		                        MCChatCustom.addCustomChat(ch, groupid, mcch.get(), user, dcp, toggles);
	                        });
                        }
                    }

                    DiscordCommandBase.registerCommands();
                    if (ResetMCCommand.resetting)
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
                    DPUtils.performNoWait(() -> {
                        try {
                            List<IMessage> msgs = genchannel.getPinnedMessages();
                            for (int i = msgs.size() - 1; i >= 10; i--) { // Unpin all pinned messages except the newest 10
                                genchannel.unpin(msgs.get(i));
                                Thread.sleep(10);
                            }
                        } catch (InterruptedException ignore) {
                        }
                    });
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
					/*if (!TBMCCoreAPI.IsTestServer()) {
						final Calendar currentCal = Calendar.getInstance();
						final Calendar newCal = Calendar.getInstance();
						currentCal.set(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH),
								currentCal.get(Calendar.DAY_OF_MONTH), 4, 10);
						if (currentCal.get(Calendar.DAY_OF_MONTH) % 9 == 0 && currentCal.before(newCal)) {
							Random rand = new Random();
							sendMessageToChannel(dc.getChannels().get(rand.nextInt(dc.getChannels().size())),
									"You could make a religion out of this");
						}
					}*/
                }
            }, 0, 10);
            for (IListener<?> listener : CommonListeners.getListeners())
                dc.getDispatcher().registerListener(listener);
            Component.registerComponent(this, new GeneralEventBroadcasterModule());
            Component.registerComponent(this, new MinecraftChatModule());
	        Component.registerComponent(this, new ExceptionListenerModule());
            TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(), this);
            TBMCChatAPI.AddCommands(this, DiscordMCCommandBase.class);
            TBMCCoreAPI.RegisterUserClass(DiscordPlayer.class);
            ChromaGamerBase.addConverter(sender -> Optional.ofNullable(sender instanceof DiscordSenderBase
                    ? ((DiscordSenderBase) sender).getChromaUser() : null));
            new Thread(this::AnnouncementGetterThreadMethod).start();
            setupProviders();
        } catch (Exception e) {
            TBMCCoreAPI.SendException("An error occured while enabling DiscordPlugin!", e);
        }
    }

    public boolean isGameRole(IRole r) {
        if (r.getGuild().getLongID() != mainServer.getLongID())
            return false; //Only allow on the main server
        val rc = new Color(149, 165, 166, 0);
        return r.getColor().equals(rc)
                && r.getPosition() < mainServer.getRoleByID(234343495735836672L).getPosition(); //Below the ChromaBot role
    }

    /**
     * Always true, except when running "stop" from console
     */
    public static boolean Restart;

    @Override
    public void pluginDisable() {
        stop = true;
	    MCChatPrivate.logoutAll();
        getConfig().set("lastannouncementtime", lastannouncementtime);
        getConfig().set("lastseentime", lastseentime);
        getConfig().set("serverup", false);

        val chcons = MCChatCustom.getCustomChats();
        val chconsc = getConfig().createSection("chcons");
        for (val chcon : chcons) {
            val chconc = chconsc.createSection(chcon.channel.getStringID());
            chconc.set("mcchid", chcon.mcchannel.ID);
            chconc.set("chid", chcon.channel.getLongID());
            chconc.set("did", chcon.user.getLongID());
            chconc.set("mcuid", chcon.dcp.getUniqueId().toString());
            chconc.set("mcname", chcon.dcp.getName());
            chconc.set("groupid", chcon.groupID);
            chconc.set("toggles", chcon.toggles);
        }

        saveConfig();
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
        try {
            SafeMode = true; // Stop interacting with Discord
            MCChatListener.stop(true);
            ChromaBot.delete();
            dc.changePresence(StatusType.IDLE, ActivityType.PLAYING, "Chromacraft"); //No longer using the same account for testing
            dc.logout();
            mainServer = devServer = null; //Fetch servers and channels again
            sent = false;
        } catch (Exception e) {
            TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e);
        }
    }

    private long lastannouncementtime = 0;
    private long lastseentime = 0;
    public static final ReactionEmoji DELIVERED_REACTION = ReactionEmoji.of("✅");

    private void AnnouncementGetterThreadMethod() {
        while (!stop) {
            try {
                if (SafeMode) {
                    Thread.sleep(10000);
                    continue;
                }
                String body = TBMCCoreAPI.DownloadString(SubredditURL + "/new/.json?limit=10");
                JsonArray json = new JsonParser().parse(body).getAsJsonObject().get("data").getAsJsonObject()
                        .get("children").getAsJsonArray();
                StringBuilder msgsb = new StringBuilder();
                StringBuilder modmsgsb = new StringBuilder();
                long lastanntime = lastannouncementtime;
                for (int i = json.size() - 1; i >= 0; i--) {
                    JsonObject item = json.get(i).getAsJsonObject();
                    final JsonObject data = item.get("data").getAsJsonObject();
                    String author = data.get("author").getAsString();
                    JsonElement distinguishedjson = data.get("distinguished");
                    String distinguished;
                    if (distinguishedjson.isJsonNull())
                        distinguished = null;
                    else
                        distinguished = distinguishedjson.getAsString();
                    String permalink = "https://www.reddit.com" + data.get("permalink").getAsString();
                    long date = data.get("created_utc").getAsLong();
                    if (date > lastseentime)
                        lastseentime = date;
                    else if (date > lastannouncementtime) {
                        do {
                            val reddituserclass = ChromaGamerBase.getTypeForFolder("reddit");
                            if (reddituserclass == null)
                                break;
                            val user = ChromaGamerBase.getUser(author, reddituserclass);
                            String id = user.getConnectedID(DiscordPlayer.class);
                            if (id != null)
                                author = "<@" + id + ">";
                        } while (false);
                        if (!author.startsWith("<"))
                            author = "/u/" + author;
                        (distinguished != null && distinguished.equals("moderator") ? modmsgsb : msgsb)
                                .append("A new post was submitted to the subreddit by ").append(author).append("\n")
                                .append(permalink).append("\n");
                        lastanntime = date;
                    }
                }
                if (msgsb.length() > 0)
                    genchannel.pin(sendMessageToChannelWait(genchannel, msgsb.toString()));
                if (modmsgsb.length() > 0)
                    sendMessageToChannel(annchannel, modmsgsb.toString());
                if (lastannouncementtime != lastanntime) {
                    lastannouncementtime = lastanntime; // If sending succeeded
                    getConfig().set("lastannouncementtime", lastannouncementtime);
                    getConfig().set("lastseentime", lastseentime);
                    saveConfig();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                Thread.sleep(10000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

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
            Bukkit.getLogger()
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
            Bukkit.getLogger().warning(
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
