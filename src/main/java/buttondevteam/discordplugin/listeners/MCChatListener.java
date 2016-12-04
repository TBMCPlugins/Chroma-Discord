package buttondevteam.discordplugin.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.DiscordSender;
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
		if (e.getSender() instanceof DiscordSender)
			return;
		if (e.getChannel().equals(Channel.GlobalChat))
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.chatchannel,
					"<" + e.getSender().getName() + "> " + e.getMessage());
	}

	private static final String[] UnconnectedCmds = new String[] { "list", "u", "shrug", "tableflip", "unflip", "mwiki" };

	public static final HashMap<String, DiscordSender> UnconnectedSenders = new HashMap<>();

	@Override // Discord
	public void handle(MessageReceivedEvent event) {
		final IUser author = event.getMessage().getAuthor();
		if (author.isBot())
			return;
		if (!event.getMessage().getChannel().getID().equals(DiscordPlugin.chatchannel.getID())
		/* && !(event.getMessage().getChannel().isPrivate() && privatechat) */)
			return;
		if (!UnconnectedSenders.containsKey(author.getID()))
			UnconnectedSenders.put(author.getID(), new DiscordSender(author));
		final DiscordSender dsender = UnconnectedSenders.get(author.getID());
		dsender.setChannel(event.getMessage().getChannel());
		if (event.getMessage().getContent().startsWith("/")) {
			final String cmd = event.getMessage().getContent().substring(1);
			Optional<? extends Player> str = Bukkit.getOnlinePlayers().stream().filter(p -> { // TODO: Support offline players
				try (DiscordPlayer dp = TBMCPlayer.getPlayerAs(p, DiscordPlayer.class)) {
					return author.getID().equals(dp.getDiscordID());
				} catch (Exception e) {
					TBMCCoreAPI.SendException("An error occured while getting Discord player for chat", e);
					return false;
				}
			}).findAny();
			try {
				if (str.isPresent()) // Connected?
				{
					// Execute as ingame player
					Bukkit.dispatchCommand(str.get(), cmd);
				} else {
					if (!Arrays.stream(UnconnectedCmds).anyMatch(s -> cmd.startsWith(s))) {
						// Command not whitelisted
						DiscordPlugin.sendMessageToChannel(event.getMessage().getChannel(), // TODO
								"Sorry, you don't have your accounts connected (or... idk, this part doesn't work yet), you can only access these commands:\n"
										+ Arrays.toString(UnconnectedCmds));
					} else
						Bukkit.dispatchCommand(dsender, cmd);
				}
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while executing command " + cmd + "!", e);
				return;
			}
		} else
			TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender,
					event.getMessage().getContent()
							+ (event.getMessage().getAttachments().size() > 0 ? event.getMessage().getAttachments()
									.stream().map(a -> a.getUrl()).collect(Collectors.joining("\n")) : ""));
	}
}
