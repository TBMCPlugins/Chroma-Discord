package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.ComponentManager;
import buttondevteam.core.component.channel.Channel;
import buttondevteam.core.component.channel.ChatRoom;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.DiscordSender;
import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.listeners.CommandListener;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import buttondevteam.lib.*;
import buttondevteam.lib.chat.ChatMessage;
import buttondevteam.lib.chat.TBMCChatAPI;
import buttondevteam.lib.player.TBMCPlayer;
import com.vdurmont.emoji.EmojiParser;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.PrivateChannel;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.spec.EmbedCreateSpec;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitTask;
import sx.blah.discord.api.internal.json.objects.EmbedObject;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.obj.User;
import sx.blah.discord.handle.obj.Message;
import sx.blah.discord.handle.obj.MessageChannel;
import sx.blah.discord.util.DiscordException;
import sx.blah.discord.util.EmbedBuilder;
import sx.blah.discord.util.MissingPermissionsException;

import java.awt.*;
import java.time.Instant;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class MCChatListener implements Listener {
	private BukkitTask sendtask;
	private LinkedBlockingQueue<AbstractMap.SimpleEntry<TBMCChatEvent, Instant>> sendevents = new LinkedBlockingQueue<>();
	private Runnable sendrunnable;
	private static Thread sendthread;
	private final MinecraftChatModule module;

	public MCChatListener(MinecraftChatModule minecraftChatModule) {
		module = minecraftChatModule;
	}

	@EventHandler // Minecraft
	public void onMCChat(TBMCChatEvent ev) {
		if (!ComponentManager.isEnabled(MinecraftChatModule.class) || ev.isCancelled()) //SafeMode: Needed so it doesn't restart after server shutdown
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

			final String authorPlayer = "[" + DPUtils.sanitizeStringNoEscape(e.getChannel().DisplayName().get()) + "] " //
				+ ("Minecraft".equals(e.getOrigin()) ? "" : "[" + e.getOrigin().substring(0, 1) + "]") //
				+ (DPUtils.sanitizeStringNoEscape(e.getSender() instanceof Player //
				? ((Player) e.getSender()).getDisplayName() //
				: e.getSender().getName()));
			val color = e.getChannel().Color().get();
			final EmbedCreateSpec embed = new EmbedBuilder().withAuthorName(authorPlayer)
				.withDescription(e.getMessage()).withColor(new Color(color.getRed(),
					color.getGreen(), color.getBlue()));
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
						+ ((DiscordSenderBase) e.getSender()).getUser().getId().asString()); // TODO: Constant/method to get URLs like this
			// embed.withFooterText(e.getChannel().DisplayName);
			embed.withTimestamp(time);
			final long nanoTime = System.nanoTime();
			InterruptibleConsumer<MCChatUtils.LastMsgData> doit = lastmsgdata -> {
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
						lastmsgdata.message.edit(mes -> mes.setEmbed(ecs -> embedObject)).block();
					} catch (MissingPermissionsException | DiscordException e1) {
						TBMCCoreAPI.SendException("An error occurred while editing chat message!", e1);
					}
			};
			// Checks if the given channel is different than where the message was sent from
			// Or if it was from MC
			Predicate<MessageChannel> isdifferentchannel = ch -> !(e.getSender() instanceof DiscordSenderBase)
				|| ((DiscordSenderBase) e.getSender()).getChannel().getId().asLong() != ch.getId().asLong();

			if (e.getChannel().isGlobal()
				&& (e.isFromCommand() || isdifferentchannel.test(module.chatChannel().get())))
				doit.accept(MCChatUtils.lastmsgdata == null
					? MCChatUtils.lastmsgdata = new MCChatUtils.LastMsgData((TextChannel) module.chatChannel().get(), null)
					: MCChatUtils.lastmsgdata);

			for (MCChatUtils.LastMsgData data : MCChatPrivate.lastmsgPerUser) {
				if ((e.isFromCommand() || isdifferentchannel.test(data.channel))
					&& e.shouldSendTo(MCChatUtils.getSender(data.channel, data.user)))
					doit.accept(data);
			}

			val iterator = MCChatCustom.lastmsgCustom.iterator();
			while (iterator.hasNext()) {
				val lmd = iterator.next();
				if ((e.isFromCommand() || isdifferentchannel.test(lmd.channel)) //Test if msg is from Discord
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
			val user = DiscordPlugin.dc.getUsers().filter(u -> u.getUsername().equals(event.getMessage().substring(startF + 1, mid)))
				.filter(u -> u.getDiscriminator().equals(event.getMessage().substring(mid + 1, end))).blockFirst();
			if (user != null) //TODO: Nicknames
				event.setMessage(event.getMessage().substring(0, startF) + "@" + user.getUsername()
					+ (event.getMessage().length() > end ? event.getMessage().substring(end) : "")); // TODO: Add formatting
			start = end; // Skip any @s inside the mention
		}
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
			MCChatUtils.lastmsgdata = null;
			MCChatPrivate.lastmsgPerUser.clear();
			MCChatCustom.lastmsgCustom.clear();
			MCChatUtils.lastmsgfromd.clear();
			MCChatUtils.ConnectedSenders.clear();
			MCChatUtils.UnconnectedSenders.clear();
			recthread = sendthread = null;
		} catch (InterruptedException e) {
			e.printStackTrace(); //This thread shouldn't be interrupted
		}
	}

	private BukkitTask rectask;
	private LinkedBlockingQueue<MessageCreateEvent> recevents = new LinkedBlockingQueue<>();
	private Runnable recrun;
	private static Thread recthread;

	// Discord
	public boolean handleDiscord(MessageCreateEvent ev) {
		if (!ComponentManager.isEnabled(MinecraftChatModule.class))
			return false;
		val author = ev.getMessage().getAuthor();
		final boolean hasCustomChat = MCChatCustom.hasCustomChat(ev.getMessage().getChannelId());
		val channel = ev.getMessage().getChannel().block();
		if (ev.getMessage().getChannelId().asLong() != module.chatChannel().get().getId().asLong()
			&& !(channel instanceof PrivateChannel
			&& author.map(u -> MCChatPrivate.isMinecraftChatEnabled(u.getId().asString())).orElse(false)
			&& !hasCustomChat))
			return false; //Chat isn't enabled on this channel
		if (channel instanceof PrivateChannel //Only in private chat
			&& ev.getMessage().getContent().isPresent()
			&& ev.getMessage().getContent().get().length() < "/mcchat<>".length()
			&& ev.getMessage().getContent().get().replace("/", "")
			.equalsIgnoreCase("mcchat")) //Either mcchat or /mcchat
			return false; //Allow disabling the chat if needed
		if (CommandListener.runCommand(ev.getMessage(), true))
			return true; //Allow running commands in chat channels
		MCChatUtils.resetLastMessage(channel);
		recevents.add(ev);
		if (rectask != null)
			return true;
		recrun = () -> { //Don't return in a while loop next time
			recthread = Thread.currentThread();
			processDiscordToMC();
			if (DiscordPlugin.plugin.isEnabled()) //Don't run again if shutting down
				rectask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, recrun); //Continue message processing
		};
		rectask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, recrun); //Start message processing
		return true;
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
			final DiscordSenderBase dsender = MCChatUtils.getSender(event.getMessage().getChannel(), sender);
			val user = dsender.getChromaUser();

			for (User u : event.getMessage().getMentions()) {
				dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
				final String nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
				dmessage = dmessage.replace(u.mention(true), "@" + (nick != null ? nick : u.getName()));
			}
			for (MessageChannel ch : event.getMessage().getChannelMentions()) {
				dmessage = dmessage.replace(ch.mention(), "#" + ch.getName()); // TODO: IG Formatting
			}

			dmessage = EmojiParser.parseToAliases(dmessage, EmojiParser.FitzpatrickAction.PARSE); //Converts emoji to text- TODO: Add option to disable (resource pack?)
			dmessage = dmessage.replaceAll(":(\\S+)\\|type_(?:(\\d)|(1)_2):", ":$1::skin-tone-$2:"); //Convert to Discord's format so it still shows up

			Function<String, String> getChatMessage = msg -> //
				msg + (event.getMessage().getAttachments().size() > 0 ? "\n" + event.getMessage()
					.getAttachments().stream().map(Message.Attachment::getUrl).collect(Collectors.joining("\n"))
					: "");

			MCChatCustom.CustomLMD clmd = MCChatCustom.getCustomChat(event.getChannel());

			boolean react = false;

			if (dmessage.startsWith("/")) { // Ingame command
				DPUtils.perform(() -> {
					if (!event.getMessage().isDeleted() && !event.getChannel().isPrivate())
						event.getMessage().delete();
				});
				final String cmd = dmessage.substring(1);
				final String cmdlowercased = cmd.toLowerCase();
				if (dsender instanceof DiscordSender && module.whitelistedCommands().get().stream()
					.noneMatch(s -> cmdlowercased.equals(s) || cmdlowercased.startsWith(s + " "))) {
					// Command not whitelisted
					dsender.sendMessage("Sorry, you can only access these commands:\n"
						+ module.whitelistedCommands().get().stream().map(uc -> "/" + uc)
						.collect(Collectors.joining(", "))
						+ (user.getConnectedID(TBMCPlayer.class) == null
						? "\nTo access your commands, first please connect your accounts, using /connect in "
						+ DPUtils.botmention()
						+ "\nThen y"
						: "\nY")
						+ "ou can access all of your regular commands (even offline) in private chat: DM me `mcchat`!");
					return;
				}
				val ev = new TBMCCommandPreprocessEvent(dsender, dmessage);
				Bukkit.getPluginManager().callEvent(ev);
				if (ev.isCancelled())
					return;
				int spi = cmdlowercased.indexOf(' ');
				final String topcmd = spi == -1 ? cmdlowercased : cmdlowercased.substring(0, spi);
				Optional<Channel> ch = Channel.getChannels()
					.filter(c -> c.ID.equalsIgnoreCase(topcmd)
						|| (c.IDs().get().length > 0
						&& Arrays.stream(c.IDs().get()).anyMatch(id -> id.equalsIgnoreCase(topcmd)))).findAny();
				if (!ch.isPresent()) //TODO: What if talking in the public chat while we have it on a different one
					Bukkit.getScheduler().runTask(DiscordPlugin.plugin, //Commands need to be run sync
						() -> { //TODO: Better handling...
							val channel = user.channel();
							val chtmp = channel.get();
							if (clmd != null) {
								channel.set(clmd.mcchannel); //Hack to send command in the channel
							} //TODO: Permcheck isn't implemented for commands
							VanillaCommandListener.runBukkitOrVanillaCommand(dsender, cmd);
							Bukkit.getLogger().info(dsender.getName() + " issued command from Discord: /" + cmdlowercased);
							if (clmd != null)
								channel.set(chtmp);
						});
				else {
					Channel chc = ch.get();
					if (!chc.isGlobal() && !event.getMessage().getChannel().isPrivate())
						dsender.sendMessage(
							"You can only talk in a public chat here. DM `mcchat` to enable private chat to talk in the other channels.");
					else {
						if (spi == -1) // Switch channels
						{
							val channel = dsender.getChromaUser().channel();
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
								+ DPUtils.sanitizeString(channel.get().DisplayName().get()));
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
			} else {// Not a command
				if (dmessage.length() == 0 && event.getMessage().getAttachments().size() == 0
					&& !event.getChannel().isPrivate() && event.getMessage().isSystemMessage()) {
					val rtr = clmd != null ? clmd.mcchannel.getRTR(clmd.dcp)
						: dsender.getChromaUser().channel().get().getRTR(dsender);
					TBMCChatAPI.SendSystemMessage(clmd != null ? clmd.mcchannel : dsender.getChromaUser().channel().get(), rtr,
						(dsender instanceof Player ? ((Player) dsender).getDisplayName()
							: dsender.getName()) + " pinned a message on Discord.", TBMCSystemChatEvent.BroadcastTarget.ALL);
				} else {
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
					val lmfd = MCChatUtils.lastmsgfromd.get(event.getChannel().getId().asLong());
					if (lmfd != null) {
						DPUtils.perform(() -> lmfd.removeReaction(DiscordPlugin.dc.getSelf(),
							DiscordPlugin.DELIVERED_REACTION)); // Remove it no matter what, we know it's there 99.99% of the time
					}
				} catch (Exception e) {
					TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e);
				}
				MCChatUtils.lastmsgfromd.put(event.getChannel().getId().asLong(), event.getMessage());
				DPUtils.perform(() -> event.getMessage().addReaction(DiscordPlugin.DELIVERED_REACTION));
			}
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e);
		}
	}

	@FunctionalInterface
	private interface InterruptibleConsumer<T> {
		void accept(T value) throws TimeoutException, InterruptedException;
	}
}
