package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.playerfaker.VCMDWrapper;
import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.UUID;

public abstract class DiscordConnectedPlayer extends DiscordSenderBase implements IMCPlayer<DiscordConnectedPlayer> {
	private @Getter VCMDWrapper<DiscordConnectedPlayer> vanillaCmdListener;
	@Getter
	@Setter
	private boolean loggedIn = false;

	@Delegate(excludes = ServerOperator.class)
	private PermissibleBase origPerm;

	private @Getter String name;

	private @Getter OfflinePlayer basePlayer;

	@Getter
	@Setter
	private PermissibleBase perm;

	private Location location = Bukkit.getWorlds().get(0).getSpawnLocation();

	private final MinecraftChatModule module;

	@Getter
	private final UUID uniqueId;

	/**
	 * The parameters must match with {@link #create(User, MessageChannel, UUID, String, MinecraftChatModule)}
	 */
	protected DiscordConnectedPlayer(User user, MessageChannel channel, UUID uuid, String mcname,
	                                 MinecraftChatModule module) {
		super(user, channel);
		origPerm = perm = new PermissibleBase(basePlayer = Bukkit.getOfflinePlayer(uuid));
		name = mcname;
		this.module = module;
		uniqueId = uuid;
		displayName = mcname;
		try {
			vanillaCmdListener = new VCMDWrapper<>(new VanillaCommandListener<>(this));
		} catch (NoClassDefFoundError e) {
			DPUtils.getLogger().warning("Vanilla commands won't be available from Discord due to a compatibility error.");
		}
	}

	public void setOp(boolean value) { //CraftPlayer-compatible implementation
		this.origPerm.setOp(value);
		this.perm.recalculatePermissions();
	}

	public boolean isOp() { return this.origPerm.isOp(); }

	@Override
	public boolean teleport(Location location) {
		if (module.allowFakePlayerTeleports().get())
			this.location = location;
		return true;
	}

	@Override
	public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
		if (module.allowFakePlayerTeleports().get())
			this.location = location;
		return true;
	}

	@Override
	public boolean teleport(Entity destination) {
		if (module.allowFakePlayerTeleports().get())
			this.location = destination.getLocation();
		return true;
	}

	@Override
	public boolean teleport(Entity destination, PlayerTeleportEvent.TeleportCause cause) {
		if (module.allowFakePlayerTeleports().get())
			this.location = destination.getLocation();
		return true;
	}

	@Override
	public Location getLocation(Location loc) {
		if (loc != null) {
			loc.setWorld(getWorld());
			loc.setX(location.getX());
			loc.setY(location.getY());
			loc.setZ(location.getZ());
			loc.setYaw(location.getYaw());
			loc.setPitch(location.getPitch());
		}

		return loc;
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public void sendRawMessage(String message) {
		sendMessage(message);
	}

	@Override
	public void chat(String msg) {
		Bukkit.getPluginManager()
			.callEvent(new AsyncPlayerChatEvent(true, this, msg, new HashSet<>(Bukkit.getOnlinePlayers())));
	}

	@Override
	public World getWorld() {
		return Bukkit.getWorlds().get(0);
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	@Override
	public Location getLocation() {
		return new Location(getWorld(), location.getX(), location.getY(), location.getZ(),
			location.getYaw(), location.getPitch());
	}

	@Getter
	@Setter
	private String displayName;

	public static DiscordConnectedPlayer create(User user, MessageChannel channel, UUID uuid, String mcname,
	                                            MinecraftChatModule module) {
		return Mockito.mock(DiscordConnectedPlayer.class, Mockito.withSettings()
			.defaultAnswer(Answers.CALLS_REAL_METHODS).useConstructor(user, channel, uuid, mcname, module));
	}
}
