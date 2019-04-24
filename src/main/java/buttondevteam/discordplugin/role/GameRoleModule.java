package buttondevteam.discordplugin.role;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import lombok.val;
import org.bukkit.Bukkit;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GameRoleModule extends Component<DiscordPlugin> {
	public List<String> GameRoles;

	@Override
	protected void enable() {
		getPlugin().getManager().registerCommand(new RoleCommand(this));
		GameRoles = DiscordPlugin.mainServer.getRoles().filter(this::isGameRole).map(Role::getName).collect(Collectors.toList()).block();
	}

	@Override
	protected void disable() {

	}

	private ConfigData<MessageChannel> logChannel() {
		return DPUtils.channelData(getConfig(), "logChannel", 239519012529111040L);
	}

	public static void handleRoleEvent(RoleEvent roleEvent) {
		val grm = ComponentManager.getIfEnabled(GameRoleModule.class);
		if (grm == null) return;
		val GameRoles = grm.GameRoles;
		val logChannel = grm.logChannel().get();
		if (roleEvent instanceof RoleCreateEvent) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
				Role role=((RoleCreateEvent) roleEvent).getRole();
				if (!grm.isGameRole(role))
					return; //Deleted or not a game role
				GameRoles.add(role.getName());
				if (logChannel != null)
					logChannel.createMessage("Added " + role.getName() + " as game role. If you don't want this, change the role's color from the default.").subscribe();
			}, 100);
		} else if (roleEvent instanceof RoleDeleteEvent) {
			Role role=((RoleDeleteEvent) roleEvent).getRole().orElse(null);
			if(role==null) return;
			if (GameRoles.remove(role.getName()) && logChannel != null)
				logChannel.createMessage("Removed " + role.getName() + " as a game role.").subscribe();
		} else if (roleEvent instanceof RoleUpdateEvent) {
			val event = (RoleUpdateEvent) roleEvent;
			if(!event.getOld().isPresent()) {
				DPUtils.getLogger().warning("Old role not stored, cannot update game role!");
				return;
			}
			Role or=event.getOld().get();
			if (!grm.isGameRole(event.getCurrent())) {
				if (GameRoles.remove(or.getName()) && logChannel != null)
					logChannel.createMessage("Removed " + or.getName() + " as a game role because it's color changed.").subscribe();
			} else {
				if (GameRoles.contains(or.getName()) && or.getName().equals(event.getCurrent().getName()))
					return;
				boolean removed = GameRoles.remove(or.getName()); //Regardless of whether it was a game role
				GameRoles.add(event.getCurrent().getName()); //Add it because it has no color
				if (logChannel != null) {
					if (removed)
						logChannel.createMessage("Changed game role from " + or.getName() + " to " + event.getCurrent().getName() + ".").subscribe();
					else
						logChannel.createMessage("Added " + event.getCurrent().getName() + " as game role because it has the default color.").subscribe();
				}
			}
		}
	}

	@SuppressWarnings("ConstantConditions")
	private boolean isGameRole(Role r) {
		if (r.getGuildId().asLong() != DiscordPlugin.mainServer.getId().asLong())
			return false; //Only allow on the main server
		val rc = new Color(149, 165, 166, 0);
		return r.getColor().equals(rc)
			&& DiscordPlugin.dc.getSelf().block().asMember(DiscordPlugin.mainServer.getId()).block().hasHigherRoles(Collections.singleton(r)).block(); //Below one of our roles
	}
}
