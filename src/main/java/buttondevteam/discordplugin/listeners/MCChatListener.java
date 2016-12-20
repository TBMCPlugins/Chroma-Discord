package buttondevteam.discordplugin.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.*;
import buttondevteam.lib.TBMCChatEvent;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCPlayer;
import buttondevteam.lib.chat.Channel;
import buttondevteam.lib.chat.TBMCChatAPI;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.MessageReceivedEvent;
import sx.blah.discord.handle.obj.IUser;

public class MCChatListener implements Listener, IListener<MessageReceivedEvent> {
	@EventHandler // Minecraft
	public void onMCChat(TBMCChatEvent e) {
		if (e.isCancelled())
			return;
		if (e.getSender() instanceof DiscordSender || e.getSender() instanceof DiscordPlayerSender)
			return;
		if (e.getChannel().equals(Channel.GlobalChat))
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
					"<" + (e.getSender() instanceof Player
							? DiscordPlugin.sanitizeString(((Player) e.getSender()).getDisplayName())
							: DiscordPlugin.sanitizeString(e.getSender().getName())) + "> "
							+ DiscordPlugin.sanitizeString(e.getMessage()));
	}

	private static final String[] UnconnectedCmds = new String[] { "list", "u", "shrug", "tableflip", "unflip", "mwiki",
			"t" };

	public static final HashMap<String, DiscordSender> UnconnectedSenders = new HashMap<>();
	public static final HashMap<String, DiscordPlayerSender> ConnectedSenders = new HashMap<>();

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		final IUser author = event.getMessage().getAuthor();
		if (author.isBot())
			return;
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.chatchannel.getID())
		/* && !(event.getMessage().getChannel().isPrivate() && privatechat) */)
			return;
		String dmessage = event.getMessage().getContent();
		try {
			Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().filter(p -> { // TODO: Support offline players
				DiscordPlayer dp = TBMCPlayer.getPlayerAs(p, DiscordPlayer.class); // Online player, already loaded
				return author.getID().equals(dp.getDiscordID());
			}).findAny();
			final DiscordSenderBase dsender;
			if (player.isPresent()) // Connected?
			{ // Execute as ingame player
				if (!ConnectedSenders.containsKey(author.getID()))
					ConnectedSenders.put(author.getID(),
							new DiscordPlayerSender(author, event.getMessage().getChannel(), player.get()));
				dsender = ConnectedSenders.get(author.getID());
			} else {
				if (!UnconnectedSenders.containsKey(author.getID()))
					UnconnectedSenders.put(author.getID(), new DiscordSender(author, event.getMessage().getChannel()));
				dsender = UnconnectedSenders.get(author.getID());
			}

			for (IUser u : event.getMessage().getMentions()) {
				dmessage = dmessage.replace(u.mention(false), "@" + u.getName()); // TODO: IG Formatting
				final Optional<String> nick = u.getNicknameForGuild(DiscordPlugin.mainServer);
				dmessage = dmessage.replace(u.mention(true), "@" + (nick.isPresent() ? nick.get() : u.getName()));
			}

			if (dmessage.startsWith("/")) {
				final String cmd = dmessage.substring(1).toLowerCase();
				if (!player.isPresent()
						&& !Arrays.stream(UnconnectedCmds).anyMatch(s -> cmd.equals(s) || cmd.startsWith(s + " "))) {
					// Command not whitelisted
					DiscordPlugin.sendMessageToChannel(event.getMessage().getChannel(), // TODO
							"Sorry, you need to be online on the server and have your accounts connected, you can only access these commands:\n"
									+ Arrays.stream(UnconnectedCmds).map(uc -> "/" + uc)
											.collect(Collectors.joining(", "))
									+ "\nTo connect your accounts, use @ChromaBot connect in "
									+ DiscordPlugin.botchannel.mention());
					return;
				}
				Bukkit.dispatchCommand(dsender, cmd);
			} else
				TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender,
						dmessage + (event.getMessage().getAttachments().size() > 0 ? "\n" + event.getMessage()
								.getAttachments().stream().map(a -> a.getUrl()).collect(Collectors.joining("\n"))
								: ""));
			event.getMessage().addReaction("white_check_mark");
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while handling " + dmessage + "!", e);
			return;
		}
	}
}
