package buttondevteam.discordplugin.role;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import discord4j.core.event.domain.role.RoleCreateEvent;
import discord4j.core.event.domain.role.RoleDeleteEvent;
import discord4j.core.event.domain.role.RoleEvent;
import discord4j.core.event.domain.role.RoleUpdateEvent;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import lombok.val;
import org.bukkit.Bukkit;
import reactor.core.publisher.Mono;

import java.awt.*;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Automatically collects roles with a certain color.
 * Users can add these roles to themselves using the /role Discord command.
 */
public class GameRoleModule extends Component<DiscordPlugin> {
	public List<String> GameRoles;

	@Override
	protected void enable() {
		getPlugin().getManager().registerCommand(new RoleCommand(this));
		GameRoles = DiscordPlugin.mainServer.getRoles().filterWhen(this::isGameRole).map(Role::getName).collect(Collectors.toList()).block();
	}

	@Override
	protected void disable() {

	}

	/**
	 * The channel where the bot logs when it detects a role change that results in a new game role or one being removed.
	 */
	private ReadOnlyConfigData<Mono<MessageChannel>> logChannel() {
		return DPUtils.channelData(getConfig(), "logChannel");
	}

	/**
	 * The role color that is used by game roles.
	 * Defaults to the second to last in the upper row - #95a5a6.
	 */
	private final ReadOnlyConfigData<Color> roleColor = getConfig().<Color>getConfig("roleColor")
		.def(new Color(149, 165, 166, 0))
		.getter(rgb -> new Color(Integer.parseInt(((String) rgb).substring(1), 16), true))
		.setter(color -> String.format("#%08x", color.getRGB())).buildReadOnly();

	public static void handleRoleEvent(RoleEvent roleEvent) {
		val grm = ComponentManager.getIfEnabled(GameRoleModule.class);
		if (grm == null) return;
		val GameRoles = grm.GameRoles;
		val logChannel = grm.logChannel().get();
		Predicate<Role> notMainServer = r -> r.getGuildId().asLong() != DiscordPlugin.mainServer.getId().asLong();
		if (roleEvent instanceof RoleCreateEvent) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
				Role role = ((RoleCreateEvent) roleEvent).getRole();
				if (notMainServer.test(role))
					return;
				grm.isGameRole(role).flatMap(b -> {
					if (!b)
						return Mono.empty(); //Deleted or not a game role
					GameRoles.add(role.getName());
					if (logChannel != null)
						return logChannel.flatMap(ch -> ch.createMessage("Added " + role.getName() + " as game role. If you don't want this, change the role's color from the game role color."));
					return Mono.empty();
				}).subscribe();
			}, 100);
		} else if (roleEvent instanceof RoleDeleteEvent) {
			Role role = ((RoleDeleteEvent) roleEvent).getRole().orElse(null);
			if (role == null) return;
			if (notMainServer.test(role))
				return;
			if (GameRoles.remove(role.getName()) && logChannel != null)
				logChannel.flatMap(ch -> ch.createMessage("Removed " + role.getName() + " as a game role.")).subscribe();
		} else if (roleEvent instanceof RoleUpdateEvent) {
			val event = (RoleUpdateEvent) roleEvent;
			if (!event.getOld().isPresent()) {
				grm.logWarn("Old role not stored, cannot update game role!");
				return;
			}
			Role or = event.getOld().get();
			if (notMainServer.test(or))
				return;
			grm.isGameRole(event.getCurrent()).flatMap(b -> {
				if (!b) {
					if (GameRoles.remove(or.getName()) && logChannel != null)
						return logChannel.flatMap(ch -> ch.createMessage("Removed " + or.getName() + " as a game role because its color changed."));
				} else {
					if (GameRoles.contains(or.getName()) && or.getName().equals(event.getCurrent().getName()))
						return Mono.empty();
					boolean removed = GameRoles.remove(or.getName()); //Regardless of whether it was a game role
					GameRoles.add(event.getCurrent().getName()); //Add it because it has no color
					if (logChannel != null) {
						if (removed)
							return logChannel.flatMap(ch -> ch.createMessage("Changed game role from " + or.getName() + " to " + event.getCurrent().getName() + "."));
						else
							return logChannel.flatMap(ch -> ch.createMessage("Added " + event.getCurrent().getName() + " as game role because it has the color of one."));
					}
				}
				return Mono.empty();
			}).subscribe();
		}
	}

	private Mono<Boolean> isGameRole(Role r) {
		if (r.getGuildId().asLong() != DiscordPlugin.mainServer.getId().asLong())
			return Mono.just(false); //Only allow on the main server
		val rc = roleColor.get();
		return Mono.just(r.getColor().equals(rc)).filter(b -> b).flatMap(b ->
			DiscordPlugin.dc.getSelf().flatMap(u -> u.asMember(DiscordPlugin.mainServer.getId()))
				.flatMap(m -> m.hasHigherRoles(Collections.singleton(r)))) //Below one of our roles
			.defaultIfEmpty(false);
	}
}
