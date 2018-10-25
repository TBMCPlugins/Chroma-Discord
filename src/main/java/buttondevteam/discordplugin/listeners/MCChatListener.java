package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import buttondevteam.lib.*;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.ChatMessage;
import buttondevteam.lib.chat.ChatRoom;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.PlayerData;
import buttondevteam.lib.player.TBMCPlayer;
import com.vdurmont.emoji.EmojiParser;
import io.netty.util.collection.LongObjectHashMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.experimental.var;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IPrivateChannel;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;

import javax.annotation.Nullable;
import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
    private BukkitTask sendtask;
    private LinkedBlockingQueue<AbstractMap.SimpleEntry<TBMCChatEvent, Instant>> sendevents = new LinkedBlockingQueue<>();
    private Runnable sendrunnable;
    private static Thread sendthread;

    @EventHandler // Minecraft
    public void onMCChat(TBMCChatEvent ev) {
        if (DiscordPlugin.SafeMode || ev.isCancelled()) //SafeMode: Needed so it doesn't restart after server shutdown
            return;
        sendevents.add(new AbstractMap.SimpleEntry<>(ev, Instant.now()));
        if (sendtask != null)
            return;
        sendrunnable = () -> {
            sendthread = Thread.currentThread();
            processMCToDiscord();
            if (DiscordPlugin.plugin.isEnabled()) //Don't run again if shutting down
                sendtask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, sendrunnable);
        };
        sendtask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, sendrunnable);
    }

    private void processMCToDiscord() {
        try {
            TBMCChatEvent e;
            Instant time;
            val se = sendevents.take(); // Wait until an element is available
            e = se.getKey();
            time = se.getValue();

            final String authorPlayer = "[" + DPUtils.sanitizeStringNoEscape(e.getChannel().DisplayName) + "] " //
                    + (e.getSender() instanceof DiscordSenderBase ? "[D]" : "") //
                    + (DPUtils.sanitizeStringNoEscape(e.getSender() instanceof Player //
                    ? ((Player) e.getSender()).getDisplayName() //
                    : e.getSender().getName()));
            final EmbedBuilder embed = new EmbedBuilder().withAuthorName(authorPlayer)
                    .withDescription(e.getMessage()).withColor(new Color(e.getChannel().color.getRed(),
                            e.getChannel().color.getGreen(), e.getChannel().color.getBlue()));
            // embed.appendField("Channel", ((e.getSender() instanceof DiscordSenderBase ? "d|" : "")
            // + DiscordPlugin.sanitizeString(e.getChannel().DisplayName)), false);
            if (e.getSender() instanceof Player)
                DPUtils.embedWithHead(
                        embed.withAuthorUrl("https://tbmcplugins.github.io/profile.html?type=minecraft&id="
                                + ((Player) e.getSender()).getUniqueId()),
                        e.getSender().getName());
            else if (e.getSender() instanceof DiscordSenderBase)
                embed.withAuthorIcon(((DiscordSenderBase) e.getSender()).getUser().getAvatarURL())
                        .withAuthorUrl("https://tbmcplugins.github.io/profile.html?type=discord&id="
                                + ((DiscordSenderBase) e.getSender()).getUser().getStringID()); // TODO: Constant/method to get URLs like this
            // embed.withFooterText(e.getChannel().DisplayName);
            embed.withTimestamp(time);
            final long nanoTime = System.nanoTime();
            InterruptibleConsumer<LastMsgData> doit = lastmsgdata -> {
                final EmbedObject embedObject = embed.build();
                if (lastmsgdata.message == null || lastmsgdata.message.isDeleted()
                        || !authorPlayer.equals(lastmsgdata.message.getEmbeds().get(0).getAuthor().getName())
                        || lastmsgdata.time / 1000000000f < nanoTime / 1000000000f - 120
                        || !lastmsgdata.mcchannel.ID.equals(e.getChannel().ID)) {
                    lastmsgdata.message = DiscordPlugin.sendMessageToChannelWait(lastmsgdata.channel, "",
                            embedObject); // TODO Use ChromaBot API
                    lastmsgdata.time = nanoTime;
                    lastmsgdata.mcchannel = e.getChannel();
                    lastmsgdata.content = embedObject.description;
                } else
                    try {
                        lastmsgdata.content = embedObject.description = lastmsgdata.content + "\n"
                                + embedObject.description;// The message object doesn't get updated
                        final LastMsgData _lastmsgdata = lastmsgdata;
                        DPUtils.perform(() -> _lastmsgdata.message.edit("", embedObject));
                    } catch (MissingPermissionsException | DiscordException e1) {
                        TBMCCoreAPI.SendException("An error occurred while editing chat message!", e1);
                    }
            };
            // Checks if the given channel is different than where the message was sent from
            // Or if it was from MC
            Predicate<IChannel> isdifferentchannel = ch -> !(e.getSender() instanceof DiscordSenderBase)
                    || ((DiscordSenderBase) e.getSender()).getChannel().getLongID() != ch.getLongID();

            if ((e.getChannel() == Channel.GlobalChat || e.getChannel().ID.equals("rp"))
                    && (e.isFromcmd() || isdifferentchannel.test(DiscordPlugin.chatchannel)))
                doit.accept(lastmsgdata == null
                        ? lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel, null)
                        : lastmsgdata);

            for (LastMsgData data : lastmsgPerUser) {
                if ((e.isFromcmd() || isdifferentchannel.test(data.channel))
                        && e.shouldSendTo(getSender(data.channel, data.user)))
                    doit.accept(data);
            }

            val iterator = lastmsgCustom.iterator();
            while (iterator.hasNext()) {
                val lmd = iterator.next();
                if ((e.isFromcmd() || isdifferentchannel.test(lmd.channel)) //Test if msg is from Discord
                        && e.getChannel().ID.equals(lmd.mcchannel.ID) //If it's from a command, the command msg has been deleted, so we need to send it
                        && e.getGroupID().equals(lmd.groupID)) { //Check if this is the group we want to test - #58
                    if (e.shouldSendTo(lmd.dcp)) //Check original user's permissions
                        doit.accept(lmd);
                    else {
                        iterator.remove(); //If the user no longer has permission, remove the connection
                        DiscordPlugin.sendMessageToChannel(lmd.channel, "The user no longer has permission to view the channel, connection removed.");
                    }
                }
            }
        } catch (InterruptedException ex) { //Stop if interrupted anywhere
            sendtask.cancel();
            sendtask = null;
        } catch (Exception ex) {
            TBMCCoreAPI.SendException("Error while sending message to Discord!", ex);
        }
    }

    @RequiredArgsConstructor
    public static class LastMsgData {
        public IMessage message;
        public long time;
        public String content;
        public final IChannel channel;
        public Channel mcchannel;
        public final IUser user;
    }

    public static class CustomLMD extends LastMsgData {
        public final String groupID;
        public final Channel mcchannel;
        public final DiscordConnectedPlayer dcp;
	    public int toggles;

        private CustomLMD(@NonNull IChannel channel, @NonNull IUser user,
                          @NonNull String groupid, @NonNull Channel mcchannel, @NonNull DiscordConnectedPlayer dcp, int toggles) {
            super(channel, user);
            groupID = groupid;
            this.mcchannel = mcchannel;
            this.dcp = dcp;
            this.toggles = toggles;
        }
    }

    @EventHandler
    public void onChatPreprocess(TBMCChatPreprocessEvent event) {
        int start = -1;
        while ((start = event.getMessage().indexOf('@', start + 1)) != -1) {
            int mid = event.getMessage().indexOf('#', start + 1);
            if (mid == -1)
                return;
            int end_ = event.getMessage().indexOf(' ', mid + 1);
            if (end_ == -1)
                end_ = event.getMessage().length();
            final int end = end_;
            final int startF = start;
            DiscordPlugin.dc.getUsersByName(event.getMessage().substring(start + 1, mid)).stream()
                    .filter(u -> u.getDiscriminator().equals(event.getMessage().substring(mid + 1, end))).findAny()
                    .ifPresent(user -> event.setMessage(event.getMessage().substring(0, startF) + "@" + user.getName()
                            + (event.getMessage().length() > end ? event.getMessage().substring(end) : ""))); // TODO: Add formatting
            start = end; // Skip any @s inside the mention
        }
    }

    private static final String[] UnconnectedCmds = new String[]{"list", "u", "shrug", "tableflip", "unflip", "mwiki",
            "yeehaw", "lenny", "rp", "plugins"};

    private static LastMsgData lastmsgdata;
    private static short lastlist = 0;
    private static short lastlistp = 0;
    /**
     * Used for messages in PMs (mcchat).
     */
    private static ArrayList<LastMsgData> lastmsgPerUser = new ArrayList<LastMsgData>();
    /**
     * Used for town or nation chats or anything else
     */
    private static ArrayList<CustomLMD> lastmsgCustom = new ArrayList<>();
    private static LongObjectHashMap<IMessage> lastmsgfromd = new LongObjectHashMap<>(); // Last message sent by a Discord user, used for clearing checkmarks

    public static boolean privateMCChat(IChannel channel, boolean start, IUser user, DiscordPlayer dp) {
        TBMCPlayer mcp = dp.getAs(TBMCPlayer.class);
        if (mcp != null) { // If the accounts aren't connected, can't make a connected sender
            val p = Bukkit.getPlayer(mcp.getUUID());
            val op = Bukkit.getOfflinePlayer(mcp.getUUID());
            if (start) {
                val sender = new DiscordConnectedPlayer(user, channel, mcp.getUUID(), op.getName());
                addSender(ConnectedSenders, user, sender);
                if (p == null)// Player is offline - If the player is online, that takes precedence
                    MCListener.callEventExcludingSome(new PlayerJoinEvent(sender, ""));
            } else {
                val sender = removeSender(ConnectedSenders, channel, user);
                if (p == null)// Player is offline - If the player is online, that takes precedence
                    MCListener.callEventExcludingSome(new PlayerQuitEvent(sender, ""));
            }
        }
        if (!start)
            lastmsgfromd.remove(channel.getLongID());
        return start //
                ? lastmsgPerUser.add(new LastMsgData(channel, user)) // Doesn't support group DMs
                : lastmsgPerUser.removeIf(lmd -> lmd.channel.getLongID() == channel.getLongID());
    }

    public static <T extends DiscordSenderBase> T addSender(HashMap<String, HashMap<IChannel, T>> senders,
                                                            IUser user, T sender) {
        return addSender(senders, user.getStringID(), sender);
    }

    public static <T extends DiscordSenderBase> T addSender(HashMap<String, HashMap<IChannel, T>> senders,
                                                            String did, T sender) {
        var map = senders.get(did);
        if (map == null)
            map = new HashMap<>();
        map.put(sender.getChannel(), sender);
        senders.put(did, map);
        return sender;
    }

    public static <T extends DiscordSenderBase> T getSender(HashMap<String, HashMap<IChannel, T>> senders,
                                                            IChannel channel, IUser user) {
        var map = senders.get(user.getStringID());
        if (map != null)
            return map.get(channel);
        return null;
    }

    public static <T extends DiscordSenderBase> T removeSender(HashMap<String, HashMap<IChannel, T>> senders,
                                                               IChannel channel, IUser user) {
        var map = senders.get(user.getStringID());
        if (map != null)
            return map.remove(channel);
        return null;
    }

    // ......................DiscordSender....DiscordConnectedPlayer.DiscordPlayerSender
    // Offline public chat......x............................................
    // Online public chat.......x...........................................x
    // Offline private chat.....x.......................x....................
    // Online private chat......x.......................x...................x
    // If online and enabling private chat, don't login
    // If leaving the server and private chat is enabled (has ConnectedPlayer), call login in a task on lowest priority
    // If private chat is enabled and joining the server, logout the fake player on highest priority
    // If online and disabling private chat, don't logout
    // The maps may not contain the senders for UnconnectedSenders

    public static boolean isMinecraftChatEnabled(DiscordPlayer dp) {
        return isMinecraftChatEnabled(dp.getDiscordID());
    }

    public static boolean isMinecraftChatEnabled(String did) { // Don't load the player data just for this
        return lastmsgPerUser.stream()
                .anyMatch(lmd -> ((IPrivateChannel) lmd.channel).getRecipient().getStringID().equals(did));
    }

    public static void addCustomChat(IChannel channel, String groupid, Channel mcchannel, IUser user, DiscordConnectedPlayer dcp, int toggles) {
        val lmd = new CustomLMD(channel, user, groupid, mcchannel, dcp, toggles);
        lastmsgCustom.add(lmd);
    }

    public static boolean hasCustomChat(IChannel channel) {
        return lastmsgCustom.stream().anyMatch(lmd -> lmd.channel.getLongID() == channel.getLongID());
    }

    public static CustomLMD getCustomChat(IChannel channel) {
        return lastmsgCustom.stream().filter(lmd -> lmd.channel.getLongID() == channel.getLongID()).findAny().orElse(null);
    }

    public static boolean removeCustomChat(IChannel channel) {
        lastmsgfromd.remove(channel.getLongID());
        return lastmsgCustom.removeIf(lmd -> lmd.channel.getLongID() == channel.getLongID());
    }

    public static List<CustomLMD> getCustomChats() {
        return Collections.unmodifiableList(lastmsgCustom);
    }

    /**
     * May contain P&lt;DiscordID&gt; as key for public chat
     */
    public static final HashMap<String, HashMap<IChannel, DiscordSender>> UnconnectedSenders = new HashMap<>();
    public static final HashMap<String, HashMap<IChannel, DiscordConnectedPlayer>> ConnectedSenders = new HashMap<>();
    /**
     * May contain P&lt;DiscordID&gt; as key for public chat
     */
    public static final HashMap<String, HashMap<IChannel, DiscordPlayerSender>> OnlineSenders = new HashMap<>();
    public static short ListC = 0;

    /**
     * Resets the last message, so it will start a new one instead of appending to it.
     * This is used when someone (even the bot) sends a message to the channel.
     *
     * @param channel The channel to reset in - the process is slightly different for the public, private and custom chats
     */
    public static void resetLastMessage(IChannel channel) {
        if (channel.getLongID() == DiscordPlugin.chatchannel.getLongID()) {
            (lastmsgdata == null ? lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel, null)
                    : lastmsgdata).message = null;
            return;
        } // Don't set the whole object to null, the player and channel information should be preserved
        for (LastMsgData data : channel.isPrivate() ? lastmsgPerUser : lastmsgCustom) {
            if (data.channel.getLongID() == channel.getLongID()) {
                data.message = null;
                return;
            }
        }
        //If it gets here, it's sending a message to a non-chat channel
    }

    public static void forAllMCChat(Consumer<IChannel> action) {
        action.accept(DiscordPlugin.chatchannel);
        for (LastMsgData data : lastmsgPerUser)
            action.accept(data.channel);
        // lastmsgCustom.forEach(cc -> action.accept(cc.channel)); - Only send relevant messages to custom chat
    }

    /**
     * For custom and all MC chat
     *
     * @param action  The action to act
     * @param toggle  The toggle to check
     * @param hookmsg Whether the message is also sent from the hook
     */
    public static void forCustomAndAllMCChat(Consumer<IChannel> action, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
        if (!DiscordPlugin.hooked || !hookmsg)
            forAllMCChat(action);
        final Consumer<CustomLMD> customLMDConsumer = cc -> action.accept(cc.channel);
        if (toggle == null)
            lastmsgCustom.forEach(customLMDConsumer);
        else
            lastmsgCustom.stream().filter(cc -> (cc.toggles & toggle.flag) != 0).forEach(customLMDConsumer);
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action The action to do
     * @param sender The sender to check perms of or null to send to all that has it toggled
     * @param toggle The toggle to check or null to send to all allowed
     */
    public static void forAllowedCustomMCChat(Consumer<IChannel> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle) {
        lastmsgCustom.stream().filter(clmd -> {
            //new TBMCChannelConnectFakeEvent(sender, clmd.mcchannel).shouldSendTo(clmd.dcp) - Thought it was this simple hehe - Wait, it *should* be this simple
            if (toggle != null && (clmd.toggles & toggle.flag) == 0)
                return false; //If null then allow
            if (sender == null)
                return true;
            val e = new TBMCChannelConnectFakeEvent(sender, clmd.mcchannel);
            return clmd.groupID.equals(e.getGroupID(sender));
        }).forEach(cc -> action.accept(cc.channel)); //TODO: Use getScore and getGroupID in fake event constructor - This should also send error messages on channel connect
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action  The action to do
     * @param sender  The sender to check perms of or null to send to all that has it toggled
     * @param toggle  The toggle to check or null to send to all allowed
     * @param hookmsg Whether the message is also sent from the hook
     */
    public static void forAllowedCustomAndAllMCChat(Consumer<IChannel> action, @Nullable CommandSender sender, @Nullable ChannelconBroadcast toggle, boolean hookmsg) {
        if (!DiscordPlugin.hooked || !hookmsg)
            forAllMCChat(action);
	    forAllowedCustomMCChat(action, sender, toggle); //TODO: Use getScore and getGroupID in fake event constructor - This should also send error messages on channel connect
    }

    public static Consumer<IChannel> send(String message) {
        return ch -> DiscordPlugin.sendMessageToChannel(ch, DPUtils.sanitizeString(message));
    }

	public static void forAllowedMCChat(Consumer<IChannel> action, TBMCSystemChatEvent event) { //TODO
        if (Channel.GlobalChat.ID.equals(event.getChannel().ID))
            action.accept(DiscordPlugin.chatchannel);
        for (LastMsgData data : lastmsgPerUser)
            if (event.shouldSendTo(getSender(data.channel, data.user)))
                action.accept(data.channel);
		lastmsgCustom.stream().filter(clmd -> {
			if ((clmd.toggles & ChannelconBroadcast.BROADCAST.flag) == 0)
				return false;
			return event.shouldSendTo(clmd.dcp);
		}).map(clmd -> clmd.channel).forEach(action);
    }

    /**
     * Stop the listener. Any calls to onMCChat will restart it as long as we're not in safe mode.
     *
     * @param wait Wait 5 seconds for the threads to stop
     */
    public static void stop(boolean wait) {
        if (sendthread != null) sendthread.interrupt();
        if (recthread != null) recthread.interrupt();
        try {
            if (sendthread != null) {
                sendthread.interrupt();
                if (wait)
                    sendthread.join(5000);
            }
            if (recthread != null) {
                recthread.interrupt();
                if (wait)
                    recthread.join(5000);
            }
            lastmsgdata = null;
            lastmsgPerUser.clear();
            lastmsgCustom.clear();
            lastmsgfromd.clear();
            ConnectedSenders.clear();
            lastlist = lastlistp = ListC = 0;
            UnconnectedSenders.clear();
            recthread = sendthread = null;
        } catch (InterruptedException e) {
            e.printStackTrace(); //This thread shouldn't be interrupted
        }
    }

    private BukkitTask rectask;
    private LinkedBlockingQueue<MessageReceivedEvent> recevents = new LinkedBlockingQueue<>();
    private Runnable recrun;
    private static Thread recthread;

    @Override // Discord
    public void handle(MessageReceivedEvent ev) {
        if (DiscordPlugin.SafeMode)
            return;
        val author = ev.getMessage().getAuthor();
        if (author.isBot())
            return;
        final boolean hasCustomChat = hasCustomChat(ev.getChannel());
        if (!ev.getMessage().getChannel().getStringID().equals(DiscordPlugin.chatchannel.getStringID())
                && !(ev.getMessage().getChannel().isPrivate() && isMinecraftChatEnabled(author.getStringID()))
                && !hasCustomChat)
            return;
        if (ev.getMessage().getContent().equalsIgnoreCase("mcchat"))
            return; // Race condition: If it gets here after it enabled mcchat it says it - I might as well allow disabling with this (CommandListener)
        if (CommandListener.runCommand(ev.getMessage(), true))
            return;
        resetLastMessage(ev.getChannel());
        lastlist++;
        recevents.add(ev);
        if (rectask != null)
            return;
        recrun = () -> { //Don't return in a while loop next time
            recthread = Thread.currentThread();
            processDiscordToMC();
            if (DiscordPlugin.plugin.isEnabled()) //Don't run again if shutting down
                rectask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, recrun); //Continue message processing
        };
        rectask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, recrun); //Start message processing
    }

    private void processDiscordToMC() {
        @val
        sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent event;
        try {
            event = recevents.take();
        } catch (InterruptedException e1) {
            rectask.cancel();
            return;
        }
        val sender = event.getMessage().getAuthor();
        String dmessage = event.getMessage().getContent();
        try {
            final DiscordSenderBase dsender = getSender(event.getMessage().getChannel(), sender);
	        val user = dsender.getChromaUser();

            for (IUser u : event.getMessage().getMentions()) {
                dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
                final String nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
                dmessage = dmessage.replace(u.mention(true), "@" + (nick != null ? nick : u.getName()));
            }
	        for (IChannel ch : event.getMessage().getChannelMentions()) {
		        dmessage = dmessage.replace(ch.mention(), "#" + ch.getName()); // TODO: IG Formatting
	        }

            dmessage = EmojiParser.parseToAliases(dmessage, EmojiParser.FitzpatrickAction.PARSE); //Converts emoji to text- TODO: Add option to disable (resource pack?)
            dmessage = dmessage.replaceAll(":(\\S+)\\|type_(?:(\\d)|(1)_2):", ":$1::skin-tone-$2:"); //Convert to Discord's format so it still shows up

            Function<String, String> getChatMessage = msg -> //
                    msg + (event.getMessage().getAttachments().size() > 0 ? "\n" + event.getMessage()
		                    .getAttachments().stream().map(IMessage.Attachment::getUrl).collect(Collectors.joining("\n"))
                            : "");

            CustomLMD clmd = getCustomChat(event.getChannel());

            boolean react = false;

            if (dmessage.startsWith("/")) { // Ingame command
                DPUtils.perform(() -> {
                    if (!event.getMessage().isDeleted() && !event.getChannel().isPrivate())
                        event.getMessage().delete();
                });
	            final String cmd = dmessage.substring(1);
	            final String cmdlowercased = cmd.toLowerCase();
                if (dsender instanceof DiscordSender && Arrays.stream(UnconnectedCmds)
                        .noneMatch(s -> cmdlowercased.equals(s) || cmdlowercased.startsWith(s + " "))) {
                    // Command not whitelisted
                    dsender.sendMessage("Sorry, you can only access these commands:\n"
                            + Arrays.stream(UnconnectedCmds).map(uc -> "/" + uc)
                            .collect(Collectors.joining(", "))
                            + (user.getConnectedID(TBMCPlayer.class) == null
                            ? "\nTo access your commands, first please connect your accounts, using /connect in "
                            + DiscordPlugin.botchannel.mention()
                            + "\nThen y"
                            : "\nY")
                            + "ou can access all of your regular commands (even offline) in private chat: DM me `mcchat`!");
                    return;
                }
                if (lastlist > 5) {
                    ListC = 0;
                    lastlist = 0;
                }
                if (cmdlowercased.equals("list") && Bukkit.getOnlinePlayers().size() == lastlistp && ListC++ > 2) // Lowered already
                {
                    dsender.sendMessage("Stop it. You know the answer.");
                    lastlist = 0;
                } else {
                    int spi = cmdlowercased.indexOf(' ');
                    final String topcmd = spi == -1 ? cmdlowercased : cmdlowercased.substring(0, spi);
                    Optional<Channel> ch = Channel.getChannels().stream()
                            .filter(c -> c.ID.equalsIgnoreCase(topcmd)
                                    || (c.IDs != null && c.IDs.length > 0
                                    && Arrays.stream(c.IDs).anyMatch(id -> id.equalsIgnoreCase(topcmd)))).findAny();
                    if (!ch.isPresent())
                        Bukkit.getScheduler().runTask(DiscordPlugin.plugin,
                                () -> {
	                                VanillaCommandListener.runBukkitOrVanillaCommand(dsender, cmd);
                                    Bukkit.getLogger().info(dsender.getName() + " issued command from Discord: /" + cmdlowercased);
                                });
                    else {
                        Channel chc = ch.get();
                        if (!chc.ID.equals(Channel.GlobalChat.ID) && !chc.ID.equals("rp") && !event.getMessage().getChannel().isPrivate())
                            dsender.sendMessage(
                                    "You can only talk in global in the public chat. DM `mcchat` to enable private chat to talk in the other channels.");
                        else {
                            if (spi == -1) // Switch channels
                            {
	                            final PlayerData<Channel> channel = dsender.getChromaUser().channel();
	                            val oldch = channel.get();
                                if (oldch instanceof ChatRoom)
                                    ((ChatRoom) oldch).leaveRoom(dsender);
                                if (!oldch.ID.equals(chc.ID)) {
	                                channel.set(chc);
                                    if (chc instanceof ChatRoom)
                                        ((ChatRoom) chc).joinRoom(dsender);
                                } else
	                                channel.set(Channel.GlobalChat);
                                dsender.sendMessage("You're now talking in: "
		                                + DPUtils.sanitizeString(channel.get().DisplayName));
                            } else { // Send single message
	                            final String msg = cmd.substring(spi + 1);
	                            val cmb = ChatMessage.builder(dsender, user, getChatMessage.apply(msg)).fromCommand(true);
                                if (clmd == null)
	                                TBMCChatAPI.SendChatMessage(cmb.build(), chc);
                                else
	                                TBMCChatAPI.SendChatMessage(cmb.permCheck(clmd.dcp).build(), chc);
                                react = true;
                            }
                        }
                    }
                }
                lastlistp = (short) Bukkit.getOnlinePlayers().size();
            } else {// Not a command
                if (dmessage.length() == 0 && event.getMessage().getAttachments().size() == 0
                        && !event.getChannel().isPrivate() && event.getMessage().isSystemMessage())
                    TBMCChatAPI.SendSystemMessage(Channel.GlobalChat, 0, "everyone",
                            (dsender instanceof Player ? ((Player) dsender).getDisplayName()
                                    : dsender.getName()) + " pinned a message on Discord.");
                else {
	                val cmb = ChatMessage.builder(dsender, user, getChatMessage.apply(dmessage)).fromCommand(false);
                    if (clmd != null)
	                    TBMCChatAPI.SendChatMessage(cmb.permCheck(clmd.dcp).build(), clmd.mcchannel);
                    else
                        TBMCChatAPI.SendChatMessage(cmb.build());
                    react = true;
                }
            }
            if (react) {
                try {
                    val lmfd = lastmsgfromd.get(event.getChannel().getLongID());
                    if (lmfd != null) {
                        DPUtils.perform(() -> lmfd.removeReaction(DiscordPlugin.dc.getOurUser(),
                                DiscordPlugin.DELIVERED_REACTION)); // Remove it no matter what, we know it's there 99.99% of the time
                    }
                } catch (Exception e) {
                    TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e);
                }
                lastmsgfromd.put(event.getChannel().getLongID(), event.getMessage());
                DPUtils.perform(() -> event.getMessage().addReaction(DiscordPlugin.DELIVERED_REACTION));
            }
        } catch (Exception e) {
            TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e);
        }
    }

    /**
     * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
     */
    private static DiscordSenderBase getSender(IChannel channel, final IUser author) {
	    //noinspection OptionalGetWithoutIsPresent
        return Stream.<Supplier<Optional<DiscordSenderBase>>>of( // https://stackoverflow.com/a/28833677/2703239
                () -> Optional.ofNullable(getSender(OnlineSenders, channel, author)), // Find first non-null
                () -> Optional.ofNullable(getSender(ConnectedSenders, channel, author)), // This doesn't support the public chat, but it'll always return null for it
		        () -> Optional.ofNullable(getSender(OnlineSenders, channel, author)), //
		        () -> Optional.of(addSender(UnconnectedSenders, author,
				        new DiscordSender(author, channel)))).map(Supplier::get).filter(Optional::isPresent).map(Optional::get).findFirst().get();
    }

    @FunctionalInterface
    private interface InterruptibleConsumer<T> {
        void accept(T value) throws TimeoutException, InterruptedException;
    }
}
