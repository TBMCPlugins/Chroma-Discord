package buttondevteam.discordplugin.role;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.discordplugin.commands.DiscordCommandBase;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import lombok.val;
import org.bukkit.Bukkit;
import sx.blah.discord.handle.impl.events.guild.role.RoleCreateEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleDeleteEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleEvent;
import sx.blah.discord.handle.impl.events.guild.role.RoleUpdateEvent;
import sx.blah.discord.handle.obj.IChannel;

public class GameRoleModule extends Component {
	@Override
	protected void enable() {
		DiscordCommandBase.registerCommand("role", new RoleCommand());
	}

	@Override
	protected void disable() {

	}

	private ConfigData<IChannel> logChannel() {
		return DPUtils.channelData(getConfig(), "logChannel", 239519012529111040L);
	}

	public static void handleRoleEvent(RoleEvent roleEvent) {
		val grm = ComponentManager.getIfEnabled(GameRoleModule.class);
		if (grm == null) return;
		if (roleEvent instanceof RoleCreateEvent) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
				if (roleEvent.getRole().isDeleted() || !DiscordPlugin.plugin.isGameRole(roleEvent.getRole()))
					return; //Deleted or not a game role
				DiscordPlugin.GameRoles.add(roleEvent.getRole().getName());
				DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Added " + roleEvent.getRole().getName() + " as game role. If you don't want this, change the role's color from the default.");
			}, 100);
		} else if (roleEvent instanceof RoleDeleteEvent) {
			if (DiscordPlugin.GameRoles.remove(roleEvent.getRole().getName()))
				DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Removed " + roleEvent.getRole().getName() + " as a game role.");
		} else if (roleEvent instanceof RoleUpdateEvent) {
			val event = (RoleUpdateEvent) roleEvent;
			if (!DiscordPlugin.plugin.isGameRole(event.getNewRole())) {
				if (DiscordPlugin.GameRoles.remove(event.getOldRole().getName()))
					DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Removed " + event.getOldRole().getName() + " as a game role because it's color changed.");
			} else {
				if (DiscordPlugin.GameRoles.contains(event.getOldRole().getName()) && event.getOldRole().getName().equals(event.getNewRole().getName()))
					return;
				boolean removed = DiscordPlugin.GameRoles.remove(event.getOldRole().getName()); //Regardless of whether it was a game role
				DiscordPlugin.GameRoles.add(event.getNewRole().getName()); //Add it because it has no color
				if (removed)
					DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Changed game role from " + event.getOldRole().getName() + " to " + event.getNewRole().getName() + ".");
				else
					DiscordPlugin.sendMessageToChannel(grm.logChannel().get(), "Added " + event.getNewRole().getName() + " as game role because it has the default color.");
			}
		}
	}
}
