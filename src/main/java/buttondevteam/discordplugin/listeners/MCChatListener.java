package buttondevteam.discordplugin.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.*;
import buttondevteam.lib.*;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.ChromaGamerBase;
import buttondevteam.lib.player.TBMCPlayer;
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
			final EmbedBuilder embed = new EmbedBuilder().withAuthorName(authorPlayer).withDescription(e.getMessage());
			final EmbedObject embedObject = e.getSender() instanceof Player
					? embed.withAuthorIcon(
							"https://minotar.net/avatar/" + ((Player) e.getSender()).getName() + "/32.png").build()
					: embed.build();
			final long nanoTime = System.nanoTime();
			Consumer<LastMsgData> doit = lastmsgdata -> {
				if (lastmsgdata.message == null || lastmsgdata.message.isDeleted()
						|| !authorPlayer.equals(lastmsgdata.message.getEmbeds().get(0).getAuthor().getName())
						|| lastmsgdata.time / 1000000000f < nanoTime / 1000000000f - 120) {
					lastmsgdata.message = DiscordPlugin.sendMessageToChannel(lastmsgdata.channel,
							lastmsgdata.channel.isPrivate() ? e.getChannel().DisplayName : "", embedObject);
					lastmsgdata.time = nanoTime;
				} else
					try {
						lastmsgdata.content = embedObject.description = lastmsgdata.content + "\n"
								+ embedObject.description;// The message object doesn't get updated
						final LastMsgData _lastmsgdata = lastmsgdata;
						DiscordPlugin.perform(() -> _lastmsgdata.message.edit("", embedObject));
					} catch (MissingPermissionsException | DiscordException e1) {
						TBMCCoreAPI.SendException("An error occured while editing chat message!", e1);
					}
			};
			if (e.getChannel().equals(Channel.GlobalChat))
				doit.accept(lastmsgdata);

			for (LastMsgData data : lastmsgPerUser) {
				final IUser iUser = data.channel.getUsersHere().stream()
						.filter(u -> u.getLongID() != u.getClient().getOurUser().getLongID()).findFirst().get(); // Doesn't support group DMs
				final DiscordPlayer user = DiscordPlayer.getUser(iUser.getStringID(), DiscordPlayer.class);
				if (user.minecraftChat().getOrDefault(false) && e.shouldSendTo(getSender(data.channel, iUser, user)))
					doit.accept(data);
			}
		} // TODO: Author URL
	}

	private static class LastMsgData {
		public IMessage message;
		public long time;
		public String content;
		public IChannel channel;

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

	private static LastMsgData lastmsgdata = new LastMsgData(DiscordPlugin.chatchannel);
	private static short lastlist = 0;
	private static short lastlistp = 0;
	/**
	 * Used for messages in PMs (mcchat).
	 */
	private static ArrayList<LastMsgData> lastmsgPerUser = new ArrayList<LastMsgData>();

	public static final HashMap<String, DiscordSender> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, DiscordPlayerSender> ConnectedSenders = new HashMap<>();
	public static short ListC = 0;

	public static void resetLastMessage() {
		lastmsgdata.message = null; // Don't set the whole object to null, the player and channel information should be preserved
	}

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		final IUser author = event.getMessage().getAuthor();
		if (!event.getMessage().getChannel().getStringID().equals(DiscordPlugin.chatchannel.getStringID())
				&& !(event.getMessage().getChannel().isPrivate() && DiscordPlayer
						.getUser(author.getStringID(), DiscordPlayer.class).minecraftChat().getOrDefault(false)))
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
				DiscordPlayer dp = ChromaGamerBase.getUser(author.getStringID(), DiscordPlayer.class);
				final DiscordSenderBase dsender = getSender(event.getMessage().getChannel(), author, dp);

				for (IUser u : event.getMessage().getMentions()) {
					dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
					final String nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
					dmessage = dmessage.replace(u.mention(true), "@" + (nick != null ? nick : u.getName()));
				}

				if (dmessage.startsWith("/")) {
					DiscordPlugin.perform(() -> {
						if (!event.getMessage().isDeleted())
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
					TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender,
							dmessage + (event.getMessage().getAttachments().size() > 0 ? "\n" + event.getMessage()
									.getAttachments().stream().map(a -> a.getUrl()).collect(Collectors.joining("\n"))
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

	private DiscordSenderBase getSender(IChannel channel, final IUser author, DiscordPlayer dp) {
		final DiscordSenderBase dsender;
		Player mcp = null; // Offline players can't really run commands, or can they? No, they can't, really.
		final String cid;
		if ((cid = dp.getConnectedID(TBMCPlayer.class)) != null // Connected?
				&& (mcp = Bukkit.getPlayer(UUID.fromString(cid))) != null) { // Execute as ingame player
			if (!ConnectedSenders.containsKey(author.getStringID()))
				ConnectedSenders.put(author.getStringID(), new DiscordPlayerSender(author, channel, mcp));
			dsender = ConnectedSenders.get(author.getStringID());
		} else {
			TBMCPlayer p = dp.getAs(TBMCPlayer.class);
			if (!UnconnectedSenders.containsKey(author.getStringID()))
				UnconnectedSenders.put(author.getStringID(),
						new DiscordSender(author, channel, p == null ? null : p.PlayerName().get())); // Display the playername, if found
			dsender = UnconnectedSenders.get(author.getStringID());
		}
		return dsender;
	}
}
