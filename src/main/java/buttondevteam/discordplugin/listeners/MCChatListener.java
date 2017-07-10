package buttondevteam.discordplugin.listeners;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import buttondevteam.discordplugin.*;
import buttondevteam.lib.*;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.TBMCPlayer;
import lombok.val;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.*;
import sx.blah.discord.util.*;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
	@EventHandler // Minecraft
	public void onMCChat(TBMCChatEvent e) {
		if (e.isCancelled())
			return;
		if (e.getSender() instanceof DiscordSender || e.getSender() instanceof DiscordPlayerSender)
			return;
		synchronized (this) {
			final String authorPlayer = DiscordPlugin.sanitizeString(e.getSender() instanceof Player //
					? ((Player) e.getSender()).getDisplayName() //
					: e.getSender().getName());
			final EmbedBuilder embed = new EmbedBuilder().withAuthorName(authorPlayer).withDescription(e.getMessage())
					.withColor(new Color(e.getChannel().color.getRed(), e.getChannel().color.getGreen(),
							e.getChannel().color.getBlue()));
			if (e.getSender() instanceof Player)
				embed.withAuthorIcon("https://minotar.net/avatar/" + ((Player) e.getSender()).getName() + "/32.png");
			final long nanoTime = System.nanoTime();
			Consumer<LastMsgData> doit = lastmsgdata -> {
				final EmbedObject embedObject = embed.build();
				String msg = lastmsgdata.channel.isPrivate() ? DiscordPlugin.sanitizeString(e.getChannel().DisplayName)
						: "";
				if (lastmsgdata.message == null || lastmsgdata.message.isDeleted()
						|| !authorPlayer.equals(lastmsgdata.message.getEmbeds().get(0).getAuthor().getName())
						|| lastmsgdata.time / 1000000000f < nanoTime / 1000000000f - 120
						|| !lastmsgdata.mcchannel.ID.equals(e.getChannel().ID)) {
					lastmsgdata.message = DiscordPlugin.sendMessageToChannel(lastmsgdata.channel, msg, embedObject);
					lastmsgdata.time = nanoTime;
					lastmsgdata.mcchannel = e.getChannel();
					lastmsgdata.content = embedObject.description;
				} else
					try {
						lastmsgdata.content = embedObject.description = lastmsgdata.content + "\n"
								+ embedObject.description;// The message object doesn't get updated
						final LastMsgData _lastmsgdata = lastmsgdata;
						DiscordPlugin.perform(() -> _lastmsgdata.message.edit(msg, embedObject));
					} catch (MissingPermissionsException | DiscordException e1) {
						TBMCCoreAPI.SendException("An error occured while editing chat message!", e1);
					}
			};
			if (e.getChannel().equals(Channel.GlobalChat))
				doit.accept(
						lastmsgdata == null ? lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel) : lastmsgdata);

			for (LastMsgData data : lastmsgPerUser) {
				final IUser iUser = data.channel.getUsersHere().stream()
						.filter(u -> u.getLongID() != u.getClient().getOurUser().getLongID()).findFirst().get(); // Doesn't support group DMs
				final DiscordPlayer user = DiscordPlayer.getUser(iUser.getStringID(), DiscordPlayer.class);
				if (user.minecraftChat().get() && e.shouldSendTo(getSender(data.channel, iUser, user)))
					doit.accept(data);
			}
		} // TODO: Author URL
	}

	private static class LastMsgData {
		public IMessage message;
		public long time;
		public String content;
		public IChannel channel;
		public Channel mcchannel;

		public LastMsgData(IChannel channel) {
			this.channel = channel;
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
							+ (event.getMessage().length() > end ? event.getMessage().substring(end)
									: ""))); // TODO: Add formatting
			start = end; // Skip any @s inside the mention
		}
	}

	private static final String[] UnconnectedCmds = new String[] { "list", "u", "shrug", "tableflip", "unflip", "mwiki",
			"yeehaw" };

	private static LastMsgData lastmsgdata;
	private static short lastlist = 0;
	private static short lastlistp = 0;
	/**
	 * Used for messages in PMs (mcchat).
	 */
	private static ArrayList<LastMsgData> lastmsgPerUser = new ArrayList<LastMsgData>();

	public static boolean privateMCChat(IChannel channel, boolean start, IUser user, DiscordPlayer dp) {
		TBMCPlayer mcp = dp.getAs(TBMCPlayer.class);
		if (mcp != null) { // If the accounts aren't connected, can't make a connected sender
			val p = Bukkit.getPlayer(mcp.getUUID());
			if (start) {
				val sender = new DiscordConnectedPlayer(user, channel, mcp.getUUID());
				ConnectedSenders.put(user.getStringID(), sender);
				if (p == null)// If the player is online, that takes precedence
					Bukkit.getPluginManager().callEvent(new PlayerJoinEvent(sender, ""));
			} else {
				val sender = ConnectedSenders.remove(user.getStringID());
				if (p == null)// If the player is online, that takes precedence
					Bukkit.getPluginManager().callEvent(new PlayerQuitEvent(sender, ""));
			}
		}
		return start //
				? lastmsgPerUser.add(new LastMsgData(channel)) //
				: lastmsgPerUser.removeIf(lmd -> lmd.channel.getLongID() == channel.getLongID());
	}

	//
	// ......................DiscordSender....DiscordConnectedPlayer.DiscordPlayerSender
	// Offline public chat......x............................................
	// Online public chat.......x...........................................x
	// Offline private chat.....x.......................x....................
	// Online private chat......x.......................x...................x
	// If online and enabling private chat, don't login
	// If leaving the server and private chat is enabled (has ConnectedPlayer), call login in a task on lowest priority
	// If private chat is enabled and joining the server, logout the fake player on highest priority
	// If online and disabling private chat, don't logout
	// The maps may not contain the senders except for DiscordPlayerSender

	public static final HashMap<String, DiscordSender> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, DiscordConnectedPlayer> ConnectedSenders = new HashMap<>();
	public static final HashMap<String, DiscordPlayerSender> OnlineSenders = new HashMap<>();
	public static short ListC = 0;

	public static void resetLastMessage() {
		(lastmsgdata == null ? lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel) : lastmsgdata).message = null; // Don't set the whole object to null, the player and channel information should
	} // be preserved

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		val author = event.getMessage().getAuthor();
		val user = DiscordPlayer.getUser(author.getStringID(), DiscordPlayer.class);
		if (!event.getMessage().getChannel().getStringID().equals(DiscordPlugin.chatchannel.getStringID())
				&& !(event.getMessage().getChannel().isPrivate() && user.minecraftChat().get()
						&& !DiscordPlugin.checkIfSomeoneIsTestingWhileWeArent()))
			return;
		resetLastMessage();
		lastlist++;
		if (author.isBot())
			return;
		if (CommandListener.runCommand(event.getMessage(), true))
			return;
		String dmessage = event.getMessage().getContent();
		synchronized (this) {
			try {
				final DiscordSenderBase dsender = getSender(event.getMessage().getChannel(), author, user);

				for (IUser u : event.getMessage().getMentions()) {
					dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
					final String nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
					dmessage = dmessage.replace(u.mention(true), "@" + (nick != null ? nick : u.getName()));
				}

				if (dmessage.startsWith("/")) {
					DiscordPlugin.perform(() -> {
						if (!event.getMessage().isDeleted() && !event.getChannel().isPrivate())
							event.getMessage().delete();
					});
					final String cmd = dmessage.substring(1).toLowerCase();
					if (dsender instanceof DiscordSender && !Arrays.stream(UnconnectedCmds)
							.anyMatch(s -> cmd.equals(s) || cmd.startsWith(s + " "))) {
						// Command not whitelisted
						dsender.sendMessage( // TODO
								"Sorry, you need to be online on the server and have your accounts connected, you can only access these commands:\n"
										+ Arrays.stream(UnconnectedCmds).map(uc -> "/" + uc)
												.collect(Collectors.joining(", "))
										+ "\nTo connect your accounts, use @ChromaBot connect in "
										+ DiscordPlugin.botchannel.mention());
						return;
					}
					if (lastlist > 5) {
						ListC = 0;
						lastlist = 0;
					}
					if (cmd.equals("list") && Bukkit.getOnlinePlayers().size() == lastlistp && ListC++ > 2) // Lowered already
					{
						dsender.sendMessage("Stop it. You know the answer.");
						lastlist = 0;
					} else
						Bukkit.dispatchCommand(dsender, cmd);
					lastlistp = (short) Bukkit.getOnlinePlayers().size();
				} else {
					if (dmessage.length() == 0 && event.getMessage().getAttachments().size() == 0)
						TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender, "pinned a message on Discord."); // TODO: Not chat message
					else
						TBMCChatAPI
								.SendChatMessage(Channel.GlobalChat,
										dsender, dmessage
												+ (event.getMessage().getAttachments().size() > 0
														? "\n" + event.getMessage().getAttachments().stream()
																.map(a -> a.getUrl()).collect(Collectors.joining("\n"))
														: ""));
					event.getMessage().getChannel().getMessageHistory().stream().forEach(m -> {
						try {
							final IReaction reaction = m.getReactionByUnicode(DiscordPlugin.DELIVERED_REACTION);
							if (reaction != null)
								DiscordPlugin.perform(() -> m.removeReaction(reaction));
						} catch (Exception e) {
							TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e);
						}
					});
					DiscordPlugin.perform(() -> event.getMessage().addReaction(DiscordPlugin.DELIVERED_REACTION));
				}
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e);
				return;
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends DiscordSenderBase> DiscordSenderBase getSender(IChannel channel, final IUser author,
			DiscordPlayer dp) {
		final DiscordSenderBase dsender;
		final Player mcp;
		final String cid;
		BiFunction<HashMap<String, T>, Supplier<T>, DiscordSenderBase> getsender = (senders, maker) -> {
			if (!senders.containsKey(author.getStringID()))
				senders.put(author.getStringID(), maker.get());
			return senders.get(author.getStringID());
		};
		if ((cid = dp.getConnectedID(TBMCPlayer.class)) != null) { // Connected?
			if ((mcp = Bukkit.getPlayer(UUID.fromString(cid))) != null) // Online? - Execute as ingame player
				dsender = getsender.apply((HashMap<String, T>) OnlineSenders,
						() -> (T) new DiscordPlayerSender(author, channel, mcp));
			else // Offline
				dsender = getsender.apply((HashMap<String, T>) ConnectedSenders,
						() -> (T) new DiscordConnectedPlayer(author, channel, UUID.fromString(cid)));
		} else { // Not connected
			TBMCPlayer p = dp.getAs(TBMCPlayer.class);
			dsender = getsender.apply((HashMap<String, T>) UnconnectedSenders,
					() -> (T) new DiscordSender(author, channel, p == null ? null : p.PlayerName().get())); // Display the playername, if found
		}
		return dsender;
	}
}
