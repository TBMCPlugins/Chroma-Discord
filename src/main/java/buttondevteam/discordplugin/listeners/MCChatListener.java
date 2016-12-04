package buttondevteam.discordplugin.listeners;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlayerSender;
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

	private static final String[] UnconnectedCmds = new String[] { "list", "u", "shrug", "tableflip", "unflip",
			"mwiki" };

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
		if (event.getMessage().getContent().startsWith("/")) {
			final String cmd = event.getMessage().getContent().substring(1);
			Optional<? extends Player> player = Bukkit.getOnlinePlayers().stream().filter(p -> { // TODO: Support offline players
				DiscordPlayer dp = TBMCPlayer.getPlayerAs(p, DiscordPlayer.class); // Online player, already loaded
				return author.getID().equals(dp.getDiscordID());
			}).findAny();
			try {
				if (player.isPresent()) // Connected?
				{
					if (!ConnectedSenders.containsKey(author.getID()))
						ConnectedSenders.put(author.getID(), new DiscordPlayerSender(author, player.get()));
					final DiscordPlayerSender dsender = ConnectedSenders.get(author.getID());
					dsender.setChannel(event.getMessage().getChannel());
					// Execute as ingame player
					Bukkit.dispatchCommand(dsender, cmd);
				} else {
					if (!UnconnectedSenders.containsKey(author.getID()))
						UnconnectedSenders.put(author.getID(), new DiscordSender(author));
					final DiscordSender dsender = UnconnectedSenders.get(author.getID());
					dsender.setChannel(event.getMessage().getChannel());
					if (!Arrays.stream(UnconnectedCmds).anyMatch(s -> cmd.startsWith(s))) {
						// Command not whitelisted
						DiscordPlugin.sendMessageToChannel(event.getMessage().getChannel(), // TODO
								"Sorry, you need to be online on the server and have your accounts connected, you can only access these commands:\n"
										+ Arrays.toString(UnconnectedCmds));
					} else
						Bukkit.dispatchCommand(dsender, cmd);
				}
			} catch (Exception e) {
				TBMCCoreAPI.SendException("An error occured while executing command " + cmd + "!", e);
				return;
			}
		} else {
			CommandSender dsender = UnconnectedSenders.get(author.getID());
			if (dsender == null)
				dsender = ConnectedSenders.get(author.getID());
			if (dsender == null) {
				UnconnectedSenders.put(author.getID(), new DiscordSender(author));
				dsender = UnconnectedSenders.get(author.getID());
			}
			TBMCChatAPI.SendChatMessage(Channel.GlobalChat, dsender,
					event.getMessage().getContent()
							+ (event.getMessage().getAttachments().size() > 0 ? event.getMessage().getAttachments()
									.stream().map(a -> a.getUrl()).collect(Collectors.joining("\n")) : ""));
		}
	}
}
