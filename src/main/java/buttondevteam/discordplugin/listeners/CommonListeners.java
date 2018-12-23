package buttondevteam.discordplugin.listeners;

import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.lib.architecture.Component;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.channel.message.MentionEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleCreateEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleUpdateEvent;
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class CommonListeners {

    /*private static ArrayList<Object> dcListeners=new ArrayList<>();

    public static void registerDiscordListener(DiscordListener listener) {
	    //Step 1: Get all events that are handled by us
	    //Step 2: Find methods that handle these
	    //...or just simply call the methods in the right order
    }

	private static void callDiscordEvent(Event event) {
		String name=event.getClass().getSimpleName();
		name=Character.toLowerCase(name.charAt(0))+name.substring(1);
		for (Object listener : dcListeners) {
			listener.getClass().getMethods(name, AsyncDiscordEvent.class);
		}
	}*/

    private static long lasttime = 0;

	/*
	MentionEvent:
	- CommandListener (starts with mention, in #bot unless 'channelcon')

	MessageReceivedEvent:
	- Minecraft chat (is enabled in the channel and message isn't [/]mcchat)
	- CommandListener (with the correct prefix in #bot, or in private)
	*/
	public static IListener<?>[] getListeners() {
		return new IListener[]{new IListener<MentionEvent>() {
			@Override
			public void handle(MentionEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				CommandListener.runCommand(event.getMessage(), true);
			}
		}, new IListener<MessageReceivedEvent>() {
			@Override
			public void handle(MessageReceivedEvent event) {
				if (DiscordPlugin.SafeMode)
					return;
				if (event.getMessage().getAuthor().isBot())
					return;
				boolean handled = false;
				val mcchat = Component.getComponents().get(MinecraftChatModule.class);
				if (mcchat != null && mcchat.isEnabled())
					handled = ((MinecraftChatModule) mcchat).getListener().handleDiscord(event);
				if (!handled)
					handled = CommandListener.runCommand(event.getMessage(), false);
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
				if (event.getRole().isDeleted() || !DiscordPlugin.plugin.isGameRole(event.getRole()))
					return; //Deleted or not a game role
				DiscordPlugin.GameRoles.add(event.getRole().getName());
				DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Added " + event.getRole().getName() + " as game role. If you don't want this, change the role's color from the default.");
			}, 100);
		}, (IListener<RoleDeleteEvent>) event -> {
			if (DiscordPlugin.GameRoles.remove(event.getRole().getName()))
				DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Removed " + event.getRole().getName() + " as a game role.");
		}, (IListener<RoleUpdateEvent>) event -> { //Role update event
			if (!DiscordPlugin.plugin.isGameRole(event.getNewRole())) {
				if (DiscordPlugin.GameRoles.remove(event.getOldRole().getName()))
					DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Removed " + event.getOldRole().getName() + " as a game role because it's color changed.");
			} else {
				if (DiscordPlugin.GameRoles.contains(event.getOldRole().getName()) && event.getOldRole().getName().equals(event.getNewRole().getName()))
					return;
				boolean removed = DiscordPlugin.GameRoles.remove(event.getOldRole().getName()); //Regardless of whether it was a game role
				DiscordPlugin.GameRoles.add(event.getNewRole().getName()); //Add it because it has no color
				if (removed)
					DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Changed game role from " + event.getOldRole().getName() + " to " + event.getNewRole().getName() + ".");
				else
					DiscordPlugin.sendMessageToChannel(DiscordPlugin.modlogchannel, "Added " + event.getNewRole().getName() + " as game role because it has the default color.");
			}
		}};
	}

    private static boolean debug = false;

    public static void debug(String debug) {
        if (CommonListeners.debug) //Debug
            DPUtils.getLogger().info(debug);
    }

    public static boolean debug() {
        return debug = !debug;
    }
}
