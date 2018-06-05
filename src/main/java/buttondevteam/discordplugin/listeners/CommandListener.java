package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import buttondevteam.lib.TBMCCoreAPI;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MentionEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleCreateEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleUpdateEvent;
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CommandListener {

	private static final String[] serverReadyStrings = new String[] { "In one week from now", // Ali
			"Between now and the heat-death of the universe.", // Ghostise
			"Soon™", "Ask again this time next month", // Ghostise
			"In about 3 seconds", // Nicolai
			"After we finish 8 plugins", // Ali
			"Tomorrow.", // Ali
			"After one tiiiny feature", // Ali
			"Next commit", // Ali
			"After we finish strangling Towny", // Ali
			"When we kill every *fucking* bug", // Ali
			"Once the server stops screaming.", // Ali
			"After HL3 comes out", // Ali
			"Next time you ask", // Ali
			"When will *you* be open?" // Ali
	};

	private static final String[] serverReadyQuestions = new String[] { "when will the server be open",
			"when will the server be ready", "when will the server be done", "when will the server be complete",
			"when will the server be finished", "when's the server ready", "when's the server open",
			"Vhen vill ze server be open?" };

	private static final Random serverReadyRandom = new Random();
	private static final ArrayList<Short> usableServerReadyStrings = new ArrayList<Short>(serverReadyStrings.length) {
		private static final long serialVersionUID = 2213771460909848770L;
		{
			createUsableServerReadyStrings(this);
		}
	};

	private static void createUsableServerReadyStrings(ArrayList<Short> list) {
		for (short i = 0; i < serverReadyStrings.length; i++)
			list.add(i);
	}

	private static long lasttime = 0;

	public static IListener<?>[] getListeners() {
		return new IListener[] { new IListener<MentionEvent>() {
			@Override
			public void handle(MentionEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				final IChannel channel = event.getMessage().getChannel();
				if (!channel.getStringID().equals(DiscordPlugin.botchannel.getStringID()))
					return;
				if (channel.getStringID().equals(DiscordPlugin.chatchannel.getStringID()))
					return; // The chat code already handles this - Right now while testing botchannel is the same as chatchannel
				event.getMessage().getChannel().setTypingStatus(true); // Fun
				runCommand(event.getMessage(), true);
			}
		}, new IListener<MessageReceivedEvent>() {
			@Override
			public void handle(MessageReceivedEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				final String msglowercase = event.getMessage().getContent().toLowerCase();
				if (!TBMCCoreAPI.IsTestServer()
						&& Arrays.stream(serverReadyQuestions).anyMatch(s -> msglowercase.contains(s))) {
					int next;
					if (usableServerReadyStrings.size() == 0)
						createUsableServerReadyStrings(usableServerReadyStrings);
					next = usableServerReadyStrings.remove(serverReadyRandom.nextInt(usableServerReadyStrings.size()));
					DiscordPlugin.sendMessageToChannel(event.getMessage().getChannel(), serverReadyStrings[next]);
					return;
				}
				if (!event.getMessage().getChannel().isPrivate()) //
					return;
				if (DiscordPlayer.getUser(event.getAuthor().getStringID(), DiscordPlayer.class)
						.isMinecraftChatEnabled())
					if (!event.getMessage().getContent().equalsIgnoreCase("mcchat"))
						return;
				if (event.getMessage().getAuthor().isBot())
					return;
				runCommand(event.getMessage(), false);
			}
		}, new IListener<sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent>() {
			@Override
			public void handle(PresenceUpdateEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				val devrole = DiscordPlugin.devServer.getRolesByName("Developer").get(0);
				if (event.getOldPresence().getStatus().equals(StatusType.OFFLINE)
						&& !event.getNewPresence().getStatus().equals(StatusType.OFFLINE)
						&& event.getUser().getRolesForGuild(DiscordPlugin.devServer).stream()
                        .anyMatch(r -> r.getLongID() == devrole.getLongID())
						&& DiscordPlugin.devServer.getUsersByRole(devrole).stream()
                        .noneMatch(u -> u.getPresence().getStatus().equals(StatusType.OFFLINE))
						&& lasttime + 10 < TimeUnit.NANOSECONDS.toHours(System.nanoTime())
						&& Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 5 == 0) {
					DiscordPlugin.sendMessageToChannel(DiscordPlugin.devofficechannel, "Full house!",
							new EmbedBuilder()
									.withImage(
											"https://cdn.discordapp.com/attachments/249295547263877121/249687682618359808/poker-hand-full-house-aces-kings-playing-cards-15553791.png")
									.build());
					lasttime = TimeUnit.NANOSECONDS.toHours(System.nanoTime());
				}
			}
        }, (IListener<RoleCreateEvent>) event -> {
            Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
                if (event.getRole().isDeleted() || event.getRole().getColor().getAlpha() != 0)
                    return; //Deleted or not a game role
                DiscordPlugin.GameRoles.add(event.getRole().getName());
                DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Added " + event.getRole().getName() + " as game role. If you don't want this, change the role's color from the default.");
            }, 100);
        }, (IListener<RoleDeleteEvent>) event -> {
            if (DiscordPlugin.GameRoles.remove(event.getRole().getName()))
                DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Removed " + event.getRole().getName() + " as a game role.");
		}, (IListener<RoleUpdateEvent>) event -> { //Role update event
			if (event.getNewRole().getColor().getAlpha() != 0)
				if (DiscordPlugin.GameRoles.remove(event.getOldRole().getName()))
                DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Removed " + event.getOldRole().getName() + " as a game role because it's color changed.");
				else {
					boolean removed = DiscordPlugin.GameRoles.remove(event.getOldRole().getName()); //Regardless of whether it was a game role
					DiscordPlugin.GameRoles.add(event.getNewRole().getName()); //Add it because it has no color
					if (removed)
						DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Changed game role from " + event.getOldRole().getName() + " to " + event.getNewRole().getName() + ".");
					else
						DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Added " + event.getNewRole().getName() + " as game role because it has no color.");
				}
        }};
	}

	/**
	 * Runs a ChromaBot command.
     *
	 * @param message
	 *            The Discord message
	 * @param mentionedonly
	 *            Only run the command if ChromaBot is mentioned at the start of the message
	 * @return Whether it ran the command (always true if mentionedonly is false)
	 */
	public static boolean runCommand(IMessage message, boolean mentionedonly) {
        debug("A");
		if (DiscordPlugin.SafeMode)
			return true;
        debug("B");
		final StringBuilder cmdwithargs = new StringBuilder(message.getContent());
		final String mention = DiscordPlugin.dc.getOurUser().mention(false);
		final String mentionNick = DiscordPlugin.dc.getOurUser().mention(true);
		boolean gotmention = checkanddeletemention(cmdwithargs, mention, message);
		gotmention = checkanddeletemention(cmdwithargs, mentionNick, message) || gotmention;
        for (String mentionRole : (Iterable<String>) message.getRoleMentions().stream().filter(r -> DiscordPlugin.dc.getOurUser().hasRole(r)).map(r -> r.mention())::iterator)
			gotmention = checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention; // Delete all mentions
        debug("C");
		if (mentionedonly && !gotmention) {
			message.getChannel().setTypingStatus(false);
			return false;
		}
        debug("D");
		message.getChannel().setTypingStatus(true);
        String cmdwithargsString = cmdwithargs.toString().trim(); //Remove spaces between mention and command
        int index = cmdwithargsString.indexOf(" ");
		String cmd;
		String args;
		if (index == -1) {
            cmd = cmdwithargsString;
			args = "";
		} else {
            cmd = cmdwithargsString.substring(0, index);
            args = cmdwithargsString.substring(index + 1).trim(); //In case there are multiple spaces
        }
        debug("E");
		DiscordCommandBase.runCommand(cmd.toLowerCase(), args, message);
		message.getChannel().setTypingStatus(false);
		return true;
	}

    private static boolean debug = false;

    public static void debug(String debug) {
        if (CommandListener.debug) //Debug
            System.out.println(debug);
    }

    public static boolean debug() {
        return debug = !debug;
    }

	private static boolean checkanddeletemention(StringBuilder cmdwithargs, String mention, IMessage message) {
		if (message.getContent().startsWith(mention)) // TODO: Resolve mentions: Compound arguments, either a mention or text
			if (cmdwithargs.length() > mention.length() + 1)
				cmdwithargs = cmdwithargs.delete(0,
						cmdwithargs.charAt(mention.length()) == ' ' ? mention.length() + 1 : mention.length());
			else
				cmdwithargs.replace(0, cmdwithargs.length(), "help");
		else
			return false;
		if (cmdwithargs.length() == 0)
			cmdwithargs.replace(0, cmdwithargs.length(), "help");
		return true;
	}
}
