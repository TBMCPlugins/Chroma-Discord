package buttondevteam.discordplugin.playerfaker;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.EntityEffect;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import buttondevteam.discordplugin.DiscordSenderBase;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

@Getter
@Setter
public abstract class DiscordEntity extends DiscordSenderBase implements Entity {
	protected DiscordEntity(IUser user, IChannel channel, int entityId, UUID uuid) {
		super(user, channel);
		this.entityId = entityId;
		uniqueId = uuid;
	}

	private HashMap<String, MetadataValue> metadata = new HashMap<String, MetadataValue>();

	@Delegate
	private PermissibleBase perm = new PermissibleBase(new ServerOperator() {
		private @Getter @Setter boolean op;
	});

	private Location location;
	private Vector velocity;
	private final int entityId;
	private EntityDamageEvent lastDamageCause;
	private final Set<String> scoreboardTags = new HashSet<String>();
	private final UUID uniqueId;

	@Override
	public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
		metadata.put(metadataKey, newMetadataValue);
	}

	@Override
	public List<MetadataValue> getMetadata(String metadataKey) {
		return Arrays.asList(metadata.get(metadataKey)); // Who needs multiple data anyways
	}

	@Override
	public boolean hasMetadata(String metadataKey) {
		return metadata.containsKey(metadataKey);
	}

	@Override
	public void removeMetadata(String metadataKey, Plugin owningPlugin) {
		metadata.remove(metadataKey);
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
	public double getHeight() {
		return 0;
	}

	@Override
	public double getWidth() {
		return 0;
	}

	@Override
	public boolean isOnGround() {
		return false;
	}

	@Override
	public World getWorld() {
		return location.getWorld();
	}

	@Override
	public boolean teleport(Location location) {
		this.location = location;
		return true;
	}

	@Override
	public boolean teleport(Location location, TeleportCause cause) {
		this.location = location;
		return true;
	}

	@Override
	public boolean teleport(Entity destination) {
		this.location = destination.getLocation();
		return true;
	}

	@Override
	public boolean teleport(Entity destination, TeleportCause cause) {
		this.location = destination.getLocation();
		return true;
	}

	@Override
	public List<Entity> getNearbyEntities(double x, double y, double z) {
		return Arrays.asList();
	}

	@Override
	public int getFireTicks() {
		return 0;
	}

	@Override
	public int getMaxFireTicks() {
		return 0;
	}

	@Override
	public void setFireTicks(int ticks) {
	}

	@Override
	public void remove() {
	}

	@Override
	public boolean isDead() { // Impossible to kill
		return false;
	}

	@Override
	public boolean isValid() {
		return true;
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public Entity getPassenger() {
		return null;
	}

	@Override
	public boolean setPassenger(Entity passenger) {
		return false;
	}

	@Override
	public List<Entity> getPassengers() {
		return Arrays.asList();
	}

	@Override
	public boolean addPassenger(Entity passenger) {
		return false;
	}

	@Override
	public boolean removePassenger(Entity passenger) { // Don't support passengers
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public boolean eject() {
		return false;
	}

	@Override
	public float getFallDistance() {
		return 0;
	}

	@Override
	public void setFallDistance(float distance) {
	}

	@Override
	public int getTicksLived() {
		return 1;
	}

	@Override
	public void setTicksLived(int value) {
	}

	@Override
	public void playEffect(EntityEffect type) {
	}

	@Override
	public boolean isInsideVehicle() {
		return false;
	}

	@Override
	public boolean leaveVehicle() {
		return false;
	}

	@Override
	public Entity getVehicle() { // Don't support vehicles
		return null;
	}

	@Override
	public void setCustomNameVisible(boolean flag) {
	}

	@Override
	public boolean isCustomNameVisible() {
		return true;
	}

	@Override
	public void setGlowing(boolean flag) {
	}

	@Override
	public boolean isGlowing() {
		return false;
	}

	@Override
	public void setInvulnerable(boolean flag) {
	}

	@Override
	public boolean isInvulnerable() {
		return true;
	}

	@Override
	public boolean isSilent() {
		return true;
	}

	@Override
	public void setSilent(boolean flag) {
	}

	@Override
	public boolean hasGravity() {
		return false;
	}

	@Override
	public void setGravity(boolean gravity) {
	}

	@Override
	public int getPortalCooldown() {
		return 0;
	}

	@Override
	public void setPortalCooldown(int cooldown) {
	}

	@Override
	public boolean addScoreboardTag(String tag) {
		return scoreboardTags.add(tag);
	}

	@Override
	public boolean removeScoreboardTag(String tag) {
		return scoreboardTags.remove(tag);
	}

	@Override
	public PistonMoveReaction getPistonMoveReaction() {
		return PistonMoveReaction.IGNORE;
	}

	@Override
	public Entity.Spigot spigot() {
		return new Entity.Spigot();
	}

}
