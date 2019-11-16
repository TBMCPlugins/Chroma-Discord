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
import java.util.stream.Collectors;

public class GameRoleModule extends Component<DiscordPlugin> {
	public List<String> GameRoles;

	@Override
	protected void enable() {
		getPlugin().getManager().registerCommand(new RoleCommand(this));
		GameRoles = DiscordPlugin.mainServer.getRoles().filterWhen(r -> isGameRole(r, false)).map(Role::getName).collect(Collectors.toList()).block();
	}

	@Override
	protected void disable() {

	}

	private ReadOnlyConfigData<Mono<MessageChannel>> logChannel() {
		return DPUtils.channelData(getConfig(), "logChannel");
	}

	public static void handleRoleEvent(RoleEvent roleEvent) {
		val grm = ComponentManager.getIfEnabled(GameRoleModule.class);
		if (grm == null) return;
		val GameRoles = grm.GameRoles;
		val logChannel = grm.logChannel().get();
		if (roleEvent instanceof RoleCreateEvent) {
			Bukkit.getScheduler().runTaskLaterAsynchronously(DiscordPlugin.plugin, () -> {
				Role role=((RoleCreateEvent) roleEvent).getRole();
				grm.isGameRole(role, false).flatMap(b -> {
					if (!b)
						return Mono.empty(); //Deleted or not a game role
					GameRoles.add(role.getName());
					if (logChannel != null)
						return logChannel.flatMap(ch -> ch.createMessage("Added " + role.getName() + " as game role. If you don't want this, change the role's color from the default."));
					return Mono.empty();
				}).subscribe();
			}, 100);
		} else if (roleEvent instanceof RoleDeleteEvent) {
			Role role=((RoleDeleteEvent) roleEvent).getRole().orElse(null);
			if(role==null) return;
			if (GameRoles.remove(role.getName()) && logChannel != null)
				logChannel.flatMap(ch -> ch.createMessage("Removed " + role.getName() + " as a game role.")).subscribe();
		} else if (roleEvent instanceof RoleUpdateEvent) {
			val event = (RoleUpdateEvent) roleEvent;
			if(!event.getOld().isPresent()) {
				DPUtils.getLogger().warning("Old role not stored, cannot update game role!");
				return;
			}
			Role or=event.getOld().get();
			grm.isGameRole(event.getCurrent(), true).flatMap(b -> {
				if (!b) {
					if (GameRoles.remove(or.getName()) && logChannel != null)
						return logChannel.flatMap(ch -> ch.createMessage("Removed " + or.getName() + " as a game role because it's color changed."));
				} else {
					if (GameRoles.contains(or.getName()) && or.getName().equals(event.getCurrent().getName()))
						return Mono.empty();
					boolean removed = GameRoles.remove(or.getName()); //Regardless of whether it was a game role
					GameRoles.add(event.getCurrent().getName()); //Add it because it has no color
					if (logChannel != null) {
						if (removed)
							return logChannel.flatMap(ch -> ch.createMessage("Changed game role from " + or.getName() + " to " + event.getCurrent().getName() + "."));
						else
							return logChannel.flatMap(ch -> ch.createMessage("Added " + event.getCurrent().getName() + " as game role because it has the default color."));
					}
				}
				return Mono.empty();
			}).subscribe();
		}
	}

	private Mono<Boolean> isGameRole(Role r, boolean debugMC) {
		boolean debug = debugMC && r.getName().equalsIgnoreCase("Minecraft");
		if (debug) TBMCCoreAPI.sendDebugMessage("Checking if Minecraft is a game role...");
		if (r.getGuildId().asLong() != DiscordPlugin.mainServer.getId().asLong()) {
			if (debug) TBMCCoreAPI.sendDebugMessage("Not in the main server: " + r.getGuildId().asString());
			return Mono.just(false); //Only allow on the main server
		}
		val rc = new Color(149, 165, 166, 0);
		if (debug) TBMCCoreAPI.sendDebugMessage("Game role color: " + rc + " - MC color: " + r.getColor());
		return Mono.just(r.getColor().equals(rc))
			.doAfterSuccessOrError((b, e) -> {
				if (debug) TBMCCoreAPI.sendDebugMessage("1. b: " + b + " - e: " + e);
			}).filter(b -> b).flatMap(b ->
				DiscordPlugin.dc.getSelf().flatMap(u -> u.asMember(DiscordPlugin.mainServer.getId()))
					.doAfterSuccessOrError((m, e) -> {
						if (debug) TBMCCoreAPI.sendDebugMessage("2. m: " + m.getDisplayName() + " e: " + e);
					}).flatMap(m -> m.hasHigherRoles(Collections.singleton(r)))) //Below one of our roles
			.doAfterSuccessOrError((b, e) -> {
				if (debug) TBMCCoreAPI.sendDebugMessage("3. b: " + b + " - e: " + e);
			}).defaultIfEmpty(false);
	}
}
