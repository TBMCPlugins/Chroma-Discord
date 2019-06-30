package buttondevteam.discordplugin;

import buttondevteam.discordplugin.playerfaker.VanillaCommandListener;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import lombok.Getter;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.PistonMoveReaction;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.*;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;

@SuppressWarnings("deprecation")
public class DiscordPlayerSender extends DiscordSenderBase implements IMCPlayer<DiscordPlayerSender> {

	protected Player player;
	private @Getter VanillaCommandListener<DiscordPlayerSender> vanillaCmdListener;

	public DiscordPlayerSender(User user, MessageChannel channel, Player player) {
		super(user, channel);
		this.player = player;
		try {
			vanillaCmdListener = new VanillaCommandListener<DiscordPlayerSender>(this);
		} catch (NoClassDefFoundError e) {
			DPUtils.getLogger().warning("Vanilla commands won't be available from Discord due to a compatibility error.");
		}
	}

	@Override
	public void sendMessage(String message) {
		player.sendMessage(message);
		super.sendMessage(message);
	}

	@Override
	public void sendMessage(String[] messages) {
		player.sendMessage(messages);
		super.sendMessage(messages);
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> arg0) {
		return player.launchProjectile(arg0);
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> arg0, Vector arg1) {
		return player.launchProjectile(arg0, arg1);
	}

	public String getCustomName() {
		return player.getCustomName();
	}

	public AttributeInstance getAttribute(Attribute attribute) {
		return player.getAttribute(attribute);
	}

	public void damage(double amount) {
		player.damage(amount);
	}

	public boolean isOp() {
		return player.isOp();
	}

	public void sendPluginMessage(Plugin source, String channel, byte[] message) {
		player.sendPluginMessage(source, channel, message);
	}

	public boolean isConversing() {
		return player.isConversing();
	}

	public boolean isPermissionSet(String name) {
		return player.isPermissionSet(name);
	}

	public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
		player.setMetadata(metadataKey, newMetadataValue);
	}

	public void damage(double amount, Entity source) {
		player.damage(amount, source);
	}

	public boolean isOnline() {
		return player.isOnline();
	}

	public void setCustomName(String name) {
		player.setCustomName(name);
	}

	public void setOp(boolean value) {
		player.setOp(value);
	}

	public void acceptConversationInput(String input) {
		player.acceptConversationInput(input);
	}

	public boolean isPermissionSet(Permission perm) {
		return player.isPermissionSet(perm);
	}

	public double getEyeHeight() {
		return player.getEyeHeight();
	}

	public String getName() {
		return player.getName();
	}

	public double getHealth() {
		return player.getHealth();
	}

	public List<MetadataValue> getMetadata(String metadataKey) {
		return player.getMetadata(metadataKey);
	}

	public Location getLocation() {
		return player.getLocation();
	}

	public boolean beginConversation(Conversation conversation) {
		return player.beginConversation(conversation);
	}

	public PlayerInventory getInventory() {
		return player.getInventory();
	}

	public boolean hasPermission(String name) {
		return player.hasPermission(name);
	}

	public double getEyeHeight(boolean ignoreSneaking) {
		return player.getEyeHeight(ignoreSneaking);
	}

	public void setHealth(double health) {
		player.setHealth(health);
	}

	public Location getLocation(Location loc) {
		return player.getLocation(loc);
	}

	public Map<String, Object> serialize() {
		return player.serialize();
	}

	public Inventory getEnderChest() {
		return player.getEnderChest();
	}

	public boolean isBanned() {
		return player.isBanned();
	}

	public void abandonConversation(Conversation conversation) {
		player.abandonConversation(conversation);
	}

	public boolean hasMetadata(String metadataKey) {
		return player.hasMetadata(metadataKey);
	}

	public String getDisplayName() {
		return player.getDisplayName();
	}

	public MainHand getMainHand() {
		return player.getMainHand();
	}

	public Location getEyeLocation() {
		return player.getEyeLocation();
	}

	public boolean isWhitelisted() {
		return player.isWhitelisted();
	}

	public boolean hasPermission(Permission perm) {
		return player.hasPermission(perm);
	}

	public Set<String> getListeningPluginChannels() {
		return player.getListeningPluginChannels();
	}

	public double getMaxHealth() {
		return player.getMaxHealth();
	}

	public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
		player.abandonConversation(conversation, details);
	}

	public void setVelocity(Vector velocity) {
		player.setVelocity(velocity);
	}

	public boolean setWindowProperty(Property prop, int value) {
		return player.setWindowProperty(prop, value);
	}

	public void setWhitelisted(boolean value) {
		player.setWhitelisted(value);
	}

	public void removeMetadata(String metadataKey, Plugin owningPlugin) {
		player.removeMetadata(metadataKey, owningPlugin);
	}

	public List<Block> getLineOfSight(Set<Material> transparent, int maxDistance) {
		return player.getLineOfSight(transparent, maxDistance);
	}

	public void setDisplayName(String name) {
		player.setDisplayName(name);
	}

	public Vector getVelocity() {
		return player.getVelocity();
	}

	public void setMaxHealth(double health) {
		player.setMaxHealth(health);
	}

	public Player getPlayer() {
		return player.getPlayer();
	}

	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return player.addAttachment(plugin, name, value);
	}

	public double getHeight() {
		return player.getHeight();
	}

	public double getWidth() {
		return player.getWidth();
	}

	public InventoryView getOpenInventory() {
		return player.getOpenInventory();
	}

	public String getPlayerListName() {
		return player.getPlayerListName();
	}

	public long getFirstPlayed() {
		return player.getFirstPlayed();
	}

	public boolean isOnGround() {
		return player.isOnGround();
	}

	public void setPlayerListName(String name) {
		player.setPlayerListName(name);
	}

	public void resetMaxHealth() {
		player.resetMaxHealth();
	}

	public InventoryView openInventory(Inventory inventory) {
		return player.openInventory(inventory);
	}

	public PermissionAttachment addAttachment(Plugin plugin) {
		return player.addAttachment(plugin);
	}

	public Block getTargetBlock(HashSet<Byte> transparent, int maxDistance) {
		return player.getTargetBlock(transparent, maxDistance);
	}

	public World getWorld() {
		return player.getWorld();
	}

	public long getLastPlayed() {
		return player.getLastPlayed();
	}

	public boolean teleport(Location location) {
		return player.teleport(location);
	}

	public InventoryView openWorkbench(Location location, boolean force) {
		return player.openWorkbench(location, force);
	}

	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return player.addAttachment(plugin, name, value, ticks);
	}

	public boolean teleport(Location location, TeleportCause cause) {
		return player.teleport(location, cause);
	}

	public Block getTargetBlock(Set<Material> transparent, int maxDistance) {
		return player.getTargetBlock(transparent, maxDistance);
	}

	public boolean hasPlayedBefore() {
		return player.hasPlayedBefore();
	}

	public InventoryView openEnchanting(Location location, boolean force) {
		return player.openEnchanting(location, force);
	}

	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return player.addAttachment(plugin, ticks);
	}

	public boolean teleport(Entity destination) {
		return player.teleport(destination);
	}

	public void setCompassTarget(Location loc) {
		player.setCompassTarget(loc);
	}

	public List<Block> getLastTwoTargetBlocks(HashSet<Byte> transparent, int maxDistance) {
		return player.getLastTwoTargetBlocks(transparent, maxDistance);
	}

	public Location getCompassTarget() {
		return player.getCompassTarget();
	}

	public InetSocketAddress getAddress() {
		return player.getAddress();
	}

	public boolean teleport(Entity destination, TeleportCause cause) {
		return player.teleport(destination, cause);
	}

	public void openInventory(InventoryView inventory) {
		player.openInventory(inventory);
	}

	public void removeAttachment(PermissionAttachment attachment) {
		player.removeAttachment(attachment);
	}

	public void sendRawMessage(String message) {
		player.sendRawMessage(message);
	}

	public InventoryView openMerchant(Villager trader, boolean force) {
		return player.openMerchant(trader, force);
	}

	public void kickPlayer(String message) {
		player.kickPlayer(message);
	}

	public List<Block> getLastTwoTargetBlocks(Set<Material> transparent, int maxDistance) {
		return player.getLastTwoTargetBlocks(transparent, maxDistance);
	}

	public void recalculatePermissions() {
		player.recalculatePermissions();
	}

	public List<Entity> getNearbyEntities(double x, double y, double z) {
		return player.getNearbyEntities(x, y, z);
	}

	public void chat(String msg) {
		player.chat(msg);
	}

	public boolean performCommand(String command) {
		return player.performCommand(command);
	}

	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return player.getEffectivePermissions();
	}

	public InventoryView openMerchant(Merchant merchant, boolean force) {
		return player.openMerchant(merchant, force);
	}

	public boolean isSneaking() {
		return player.isSneaking();
	}

	public int getEntityId() {
		return player.getEntityId();
	}

	public void setSneaking(boolean sneak) {
		player.setSneaking(sneak);
	}

	public int getFireTicks() {
		return player.getFireTicks();
	}

	public int getRemainingAir() {
		return player.getRemainingAir();
	}

	public boolean isSprinting() {
		return player.isSprinting();
	}

	public int getMaxFireTicks() {
		return player.getMaxFireTicks();
	}

	public void setRemainingAir(int ticks) {
		player.setRemainingAir(ticks);
	}

	public void setSprinting(boolean sprinting) {
		player.setSprinting(sprinting);
	}

	public void setFireTicks(int ticks) {
		player.setFireTicks(ticks);
	}

	public void closeInventory() {
		player.closeInventory();
	}

	public int getMaximumAir() {
		return player.getMaximumAir();
	}

	public ItemStack getItemInHand() {
		return player.getItemInHand();
	}

	public void saveData() {
		player.saveData();
	}

	public void remove() {
		player.remove();
	}

	public void setMaximumAir(int ticks) {
		player.setMaximumAir(ticks);
	}

	public boolean isDead() {
		return player.isDead();
	}

	public void loadData() {
		player.loadData();
	}

	public boolean isValid() {
		return player.isValid();
	}

	public int getMaximumNoDamageTicks() {
		return player.getMaximumNoDamageTicks();
	}

	public void setItemInHand(ItemStack item) {
		player.setItemInHand(item);
	}

	public Server getServer() {
		return player.getServer();
	}

	public void setMaximumNoDamageTicks(int ticks) {
		player.setMaximumNoDamageTicks(ticks);
	}

	public void setSleepingIgnored(boolean isSleeping) {
		player.setSleepingIgnored(isSleeping);
	}

	public Entity getPassenger() {
		return player.getPassenger();
	}

	public ItemStack getItemOnCursor() {
		return player.getItemOnCursor();
	}

	public double getLastDamage() {
		return player.getLastDamage();
	}

	public boolean setPassenger(Entity passenger) {
		return player.setPassenger(passenger);
	}

	public void setItemOnCursor(ItemStack item) {
		player.setItemOnCursor(item);
	}

	public boolean isSleepingIgnored() {
		return player.isSleepingIgnored();
	}

	public void setLastDamage(double damage) {
		player.setLastDamage(damage);
	}

	public void playNote(Location loc, byte instrument, byte note) {
		player.playNote(loc, instrument, note);
	}

	public boolean hasCooldown(Material material) {
		return player.hasCooldown(material);
	}

	public int getNoDamageTicks() {
		return player.getNoDamageTicks();
	}

	public List<Entity> getPassengers() {
		return player.getPassengers();
	}

	public void setNoDamageTicks(int ticks) {
		player.setNoDamageTicks(ticks);
	}

	public int getCooldown(Material material) {
		return player.getCooldown(material);
	}

	public void playNote(Location loc, Instrument instrument, Note note) {
		player.playNote(loc, instrument, note);
	}

	public Player getKiller() {
		return player.getKiller();
	}

	public boolean addPassenger(Entity passenger) {
		return player.addPassenger(passenger);
	}

	public void setCooldown(Material material, int ticks) {
		player.setCooldown(material, ticks);
	}

	public boolean addPotionEffect(PotionEffect effect) {
		return player.addPotionEffect(effect);
	}

	public boolean removePassenger(Entity passenger) {
		return player.removePassenger(passenger);
	}

	public void playSound(Location location, Sound sound, float volume, float pitch) {
		player.playSound(location, sound, volume, pitch);
	}

	public boolean isEmpty() {
		return player.isEmpty();
	}

	public boolean addPotionEffect(PotionEffect effect, boolean force) {
		return player.addPotionEffect(effect, force);
	}

	public boolean eject() {
		return player.eject();
	}

	public boolean isSleeping() {
		return player.isSleeping();
	}

	public float getFallDistance() {
		return player.getFallDistance();
	}

	public void playSound(Location location, String sound, float volume, float pitch) {
		player.playSound(location, sound, volume, pitch);
	}

	public int getSleepTicks() {
		return player.getSleepTicks();
	}

	public void setFallDistance(float distance) {
		player.setFallDistance(distance);
	}

	public boolean addPotionEffects(Collection<PotionEffect> effects) {
		return player.addPotionEffects(effects);
	}

	public GameMode getGameMode() {
		return player.getGameMode();
	}

	public void setLastDamageCause(EntityDamageEvent event) {
		player.setLastDamageCause(event);
	}

	public void setGameMode(GameMode mode) {
		player.setGameMode(mode);
	}

	public EntityDamageEvent getLastDamageCause() {
		return player.getLastDamageCause();
	}

	public boolean hasPotionEffect(PotionEffectType type) {
		return player.hasPotionEffect(type);
	}

	public boolean isBlocking() {
		return player.isBlocking();
	}

	public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
		player.playSound(location, sound, category, volume, pitch);
	}

	public boolean isHandRaised() {
		return player.isHandRaised();
	}

	public UUID getUniqueId() {
		return player.getUniqueId();
	}

	public PotionEffect getPotionEffect(PotionEffectType type) {
		return player.getPotionEffect(type);
	}

	public int getTicksLived() {
		return player.getTicksLived();
	}

	public int getExpToLevel() {
		return player.getExpToLevel();
	}

	public Entity getShoulderEntityLeft() {
		return player.getShoulderEntityLeft();
	}

	public void setTicksLived(int value) {
		player.setTicksLived(value);
	}

	public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
		player.playSound(location, sound, category, volume, pitch);
	}

	public void removePotionEffect(PotionEffectType type) {
		player.removePotionEffect(type);
	}

	public void playEffect(EntityEffect type) {
		player.playEffect(type);
	}

	public Collection<PotionEffect> getActivePotionEffects() {
		return player.getActivePotionEffects();
	}

	public void setShoulderEntityLeft(Entity entity) {
		player.setShoulderEntityLeft(entity);
	}

	public boolean hasLineOfSight(Entity other) {
		return player.hasLineOfSight(other);
	}

	public EntityType getType() {
		return player.getType();
	}

	public boolean isInsideVehicle() {
		return player.isInsideVehicle();
	}

	public void stopSound(Sound sound) {
		player.stopSound(sound);
	}

	public boolean leaveVehicle() {
		return player.leaveVehicle();
	}

	public void stopSound(String sound) {
		player.stopSound(sound);
	}

	public boolean getRemoveWhenFarAway() {
		return player.getRemoveWhenFarAway();
	}

	public void stopSound(Sound sound, SoundCategory category) {
		player.stopSound(sound, category);
	}

	public Entity getVehicle() {
		return player.getVehicle();
	}

	public Entity getShoulderEntityRight() {
		return player.getShoulderEntityRight();
	}

	public void setRemoveWhenFarAway(boolean remove) {
		player.setRemoveWhenFarAway(remove);
	}

	public void stopSound(String sound, SoundCategory category) {
		player.stopSound(sound, category);
	}

	public void setCustomNameVisible(boolean flag) {
		player.setCustomNameVisible(flag);
	}

	public EntityEquipment getEquipment() {
		return player.getEquipment();
	}

	public void playEffect(Location loc, Effect effect, int data) {
		player.playEffect(loc, effect, data);
	}

	public void setCanPickupItems(boolean pickup) {
		player.setCanPickupItems(pickup);
	}

	public void setShoulderEntityRight(Entity entity) {
		player.setShoulderEntityRight(entity);
	}

	public boolean isCustomNameVisible() {
		return player.isCustomNameVisible();
	}

	public <T> void playEffect(Location loc, Effect effect, T data) {
		player.playEffect(loc, effect, data);
	}

	public boolean getCanPickupItems() {
		return player.getCanPickupItems();
	}

	public void setGlowing(boolean flag) {
		player.setGlowing(flag);
	}

	public boolean isLeashed() {
		return player.isLeashed();
	}

	public void sendBlockChange(Location loc, Material material, byte data) {
		player.sendBlockChange(loc, material, data);
	}

	public boolean isGlowing() {
		return player.isGlowing();
	}

	public Entity getLeashHolder() throws IllegalStateException {
		return player.getLeashHolder();
	}

	public void setInvulnerable(boolean flag) {
		player.setInvulnerable(flag);
	}

	public boolean setLeashHolder(Entity holder) {
		return player.setLeashHolder(holder);
	}

	public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
		return player.sendChunkChange(loc, sx, sy, sz, data);
	}

	public boolean isInvulnerable() {
		return player.isInvulnerable();
	}

	public boolean isSilent() {
		return player.isSilent();
	}

	public void setSilent(boolean flag) {
		player.setSilent(flag);
	}

	public boolean isGliding() {
		return player.isGliding();
	}

	public void setGliding(boolean gliding) {
		player.setGliding(gliding);
	}

	public boolean hasGravity() {
		return player.hasGravity();
	}

	public void setGravity(boolean gravity) {
		player.setGravity(gravity);
	}

	public void sendBlockChange(Location loc, int material, byte data) {
		player.sendBlockChange(loc, material, data);
	}

	public void setAI(boolean ai) {
		player.setAI(ai);
	}

	public int getPortalCooldown() {
		return player.getPortalCooldown();
	}

	public boolean hasAI() {
		return player.hasAI();
	}

	public void setPortalCooldown(int cooldown) {
		player.setPortalCooldown(cooldown);
	}

	public void setCollidable(boolean collidable) {
		player.setCollidable(collidable);
	}

	public Set<String> getScoreboardTags() {
		return player.getScoreboardTags();
	}

	public void sendSignChange(Location loc, String[] lines) throws IllegalArgumentException {
		player.sendSignChange(loc, lines);
	}

	public boolean addScoreboardTag(String tag) {
		return player.addScoreboardTag(tag);
	}

	public boolean isCollidable() {
		return player.isCollidable();
	}

	public boolean removeScoreboardTag(String tag) {
		return player.removeScoreboardTag(tag);
	}

	public PistonMoveReaction getPistonMoveReaction() {
		return player.getPistonMoveReaction();
	}

	public void sendMap(MapView map) {
		player.sendMap(map);
	}

	public void updateInventory() {
		player.updateInventory();
	}

	public void awardAchievement(Achievement achievement) {
		player.awardAchievement(achievement);
	}

	public void removeAchievement(Achievement achievement) {
		player.removeAchievement(achievement);
	}

	public boolean hasAchievement(Achievement achievement) {
		return player.hasAchievement(achievement);
	}

	public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
		player.incrementStatistic(statistic);
	}

	public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
		player.decrementStatistic(statistic);
	}

	public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
		player.incrementStatistic(statistic, amount);
	}

	public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
		player.decrementStatistic(statistic, amount);
	}

	public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
		player.setStatistic(statistic, newValue);
	}

	public int getStatistic(Statistic statistic) throws IllegalArgumentException {
		return player.getStatistic(statistic);
	}

	public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
		player.incrementStatistic(statistic, material);
	}

	public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
		player.decrementStatistic(statistic, material);
	}

	public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
		return player.getStatistic(statistic, material);
	}

	public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
		player.incrementStatistic(statistic, material, amount);
	}

	public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
		player.decrementStatistic(statistic, material, amount);
	}

	public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
		player.setStatistic(statistic, material, newValue);
	}

	public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
		player.incrementStatistic(statistic, entityType);
	}

	public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
		player.decrementStatistic(statistic, entityType);
	}

	public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
		return player.getStatistic(statistic, entityType);
	}

	public void incrementStatistic(Statistic statistic, EntityType entityType, int amount)
			throws IllegalArgumentException {
		player.incrementStatistic(statistic, entityType, amount);
	}

	public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
		player.decrementStatistic(statistic, entityType, amount);
	}

	public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
		player.setStatistic(statistic, entityType, newValue);
	}

	public void setPlayerTime(long time, boolean relative) {
		player.setPlayerTime(time, relative);
	}

	public long getPlayerTime() {
		return player.getPlayerTime();
	}

	public long getPlayerTimeOffset() {
		return player.getPlayerTimeOffset();
	}

	public boolean isPlayerTimeRelative() {
		return player.isPlayerTimeRelative();
	}

	public void resetPlayerTime() {
		player.resetPlayerTime();
	}

	public void setPlayerWeather(WeatherType type) {
		player.setPlayerWeather(type);
	}

	public WeatherType getPlayerWeather() {
		return player.getPlayerWeather();
	}

	public void resetPlayerWeather() {
		player.resetPlayerWeather();
	}

	public void giveExp(int amount) {
		player.giveExp(amount);
	}

	public void giveExpLevels(int amount) {
		player.giveExpLevels(amount);
	}

	public float getExp() {
		return player.getExp();
	}

	public void setExp(float exp) {
		player.setExp(exp);
	}

	public int getLevel() {
		return player.getLevel();
	}

	public void setLevel(int level) {
		player.setLevel(level);
	}

	public int getTotalExperience() {
		return player.getTotalExperience();
	}

	public void setTotalExperience(int exp) {
		player.setTotalExperience(exp);
	}

	public float getExhaustion() {
		return player.getExhaustion();
	}

	public void setExhaustion(float value) {
		player.setExhaustion(value);
	}

	public float getSaturation() {
		return player.getSaturation();
	}

	public void setSaturation(float value) {
		player.setSaturation(value);
	}

	public int getFoodLevel() {
		return player.getFoodLevel();
	}

	public void setFoodLevel(int value) {
		player.setFoodLevel(value);
	}

	public Location getBedSpawnLocation() {
		return player.getBedSpawnLocation();
	}

	public void setBedSpawnLocation(Location location) {
		player.setBedSpawnLocation(location);
	}

	public void setBedSpawnLocation(Location location, boolean force) {
		player.setBedSpawnLocation(location, force);
	}

	public boolean getAllowFlight() {
		return player.getAllowFlight();
	}

	public void setAllowFlight(boolean flight) {
		player.setAllowFlight(flight);
	}

	public void hidePlayer(Player player) {
		player.hidePlayer(player);
	}

	public void showPlayer(Player player) {
		player.showPlayer(player);
	}

	public boolean canSee(Player player) {
		return player.canSee(player);
	}

	public boolean isFlying() {
		return player.isFlying();
	}

	public void setFlying(boolean value) {
		player.setFlying(value);
	}

	public void setFlySpeed(float value) throws IllegalArgumentException {
		player.setFlySpeed(value);
	}

	public void setWalkSpeed(float value) throws IllegalArgumentException {
		player.setWalkSpeed(value);
	}

	public float getFlySpeed() {
		return player.getFlySpeed();
	}

	public float getWalkSpeed() {
		return player.getWalkSpeed();
	}

	public void setTexturePack(String url) {
		player.setTexturePack(url);
	}

	public void setResourcePack(String url) {
		player.setResourcePack(url);
	}

	public void setResourcePack(String url, byte[] hash) {
		player.setResourcePack(url, hash);
	}

	public Scoreboard getScoreboard() {
		return player.getScoreboard();
	}

	public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
		player.setScoreboard(scoreboard);
	}

	public boolean isHealthScaled() {
		return player.isHealthScaled();
	}

	public void setHealthScaled(boolean scale) {
		player.setHealthScaled(scale);
	}

	public void setHealthScale(double scale) throws IllegalArgumentException {
		player.setHealthScale(scale);
	}

	public double getHealthScale() {
		return player.getHealthScale();
	}

	public Entity getSpectatorTarget() {
		return player.getSpectatorTarget();
	}

	public void setSpectatorTarget(Entity entity) {
		player.setSpectatorTarget(entity);
	}

	public void sendTitle(String title, String subtitle) {
		player.sendTitle(title, subtitle);
	}

	public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		player.sendTitle(title, subtitle, fadeIn, stay, fadeOut);
	}

	public void resetTitle() {
		player.resetTitle();
	}

	public void spawnParticle(Particle particle, Location location, int count) {
		player.spawnParticle(particle, location, count);
	}

	public void spawnParticle(Particle particle, double x, double y, double z, int count) {
		player.spawnParticle(particle, x, y, z, count);
	}

	public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
		player.spawnParticle(particle, location, count, data);
	}

	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
		player.spawnParticle(particle, x, y, z, count, data);
	}

	public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ) {
		player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ);
	}

	public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ) {
		player.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ);
	}

	public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, T data) {
		player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, data);
	}

	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, T data) {
		player.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, data);
	}

	public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, double extra) {
		player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra);
	}

	public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, double extra) {
		player.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra);
	}

	public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, double extra, T data) {
		player.spawnParticle(particle, location, count, offsetX, offsetY, offsetZ, extra, data);
	}

	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, double extra, T data) {
		player.spawnParticle(particle, x, y, z, count, offsetX, offsetY, offsetZ, extra, data);
	}

	public AdvancementProgress getAdvancementProgress(Advancement advancement) {
		return player.getAdvancementProgress(advancement);
	}

	public String getLocale() {
		return player.getLocale();
	}

	public org.bukkit.entity.Player.Spigot spigot() {
		return player.spigot();
	}
}
