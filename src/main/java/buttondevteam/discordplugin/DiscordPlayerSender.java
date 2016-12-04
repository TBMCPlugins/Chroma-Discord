package buttondevteam.discordplugin;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.stream.Collectors;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.*;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;

import buttondevteam.lib.TBMCCoreAPI;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

@SuppressWarnings("deprecation")
public class DiscordPlayerSender implements Player {
	private IUser user;
	private IChannel channel;
	private Player player;

	public DiscordPlayerSender(IUser user, Player player) {
		this.user = user;
		this.player = player;
	}

	public IChannel getChannel() {
		return channel;
	}

	public void setChannel(IChannel channel) {
		this.channel = channel;
	}

	@Override
	public boolean isPermissionSet(String name) {
		return player.isPermissionSet(name);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return this.player.isPermissionSet(perm);
	}

	@Override
	public boolean hasPermission(String name) {
		return player.hasPermission(name);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return this.player.hasPermission(perm);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return player.addAttachment(plugin, name, value);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return player.addAttachment(plugin);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return player.addAttachment(plugin, name, value, ticks);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return player.addAttachment(plugin, ticks);
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		player.removeAttachment(attachment);
	}

	@Override
	public void recalculatePermissions() {
		player.recalculatePermissions();
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return player.getEffectivePermissions();
	}

	@Override
	public boolean isOp() { // TODO: Connect with TBMC acc
		return player.isOp();
	}

	@Override
	public void setOp(boolean value) { // TODO: Connect with TBMC acc
		player.setOp(value);
	}

	@Override
	public void sendMessage(String message) {
		try {
			final boolean broadcast = new Exception().getStackTrace()[2].getMethodName().contains("broadcast");
			String sanitizedMsg = "";
			for (int i = 0; i < message.length(); i++) {
				if (message.charAt(i) != '§') {
					sanitizedMsg += message.charAt(i);
				} else {
					i++;
				}
			}
			final String sendmsg = sanitizedMsg;
			Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin, () -> DiscordPlugin
					.sendMessageToChannel(channel, (broadcast ? user.mention() + " " : "") + sendmsg));
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while sending message to DiscordSender", e);
		}
	}

	@Override
	public void sendMessage(String[] messages) {
		sendMessage(Arrays.stream(messages).collect(Collectors.joining("\n")));
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public String getName() {
		return user.getDisplayName(DiscordPlugin.mainServer);
	}

	// Find: " (\w+)\(\) \{\s+\/\/ TO\DO Auto-generated method stub\s+return null;" - Replace: " $1() { return player.$1();"
	@Override
	public String getDisplayName() {
		return player.getDisplayName();
	}

	// Find: " (\w+)\((\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3) { player.$1($3);"
	@Override
	public void setDisplayName(String name) {
		player.setDisplayName(name);
	}

	@Override
	public String getPlayerListName() {
		return player.getPlayerListName();
	}

	@Override
	public void setPlayerListName(String name) {
		player.setPlayerListName(name);
	}

	@Override
	public void setCompassTarget(Location loc) {
		player.setCompassTarget(loc);
	}

	@Override
	public Location getCompassTarget() {
		return player.getCompassTarget();
	}

	@Override
	public InetSocketAddress getAddress() {
		return player.getAddress();
	}

	@Override
	public void sendRawMessage(String message) {
		player.sendRawMessage(message);
	}

	@Override
	public void kickPlayer(String message) {
		player.kickPlayer(message);
	}

	@Override
	public void chat(String msg) {
		player.chat(msg);
	}

	@Override
	public boolean performCommand(String command) {
		player.performCommand(command);
		return false;
	}

	// Find: " (\w+)\(\) \{\s+\/\/ TO\DO Auto-generated method stub\s+return false;" - Replace: " $1() { return player.$1();"
	@Override
	public boolean isSneaking() {
		return player.isSneaking();
	}

	@Override
	public void setSneaking(boolean sneak) {
		player.setSneaking(sneak);
	}

	@Override
	public boolean isSprinting() {
		return player.isSprinting();
	}

	@Override
	public void setSprinting(boolean sprinting) {
		player.setSprinting(sprinting);
	}

	@Override
	public void saveData() {
		player.saveData();
	}

	@Override
	public void loadData() {
		player.loadData();
	}

	@Override
	public void setSleepingIgnored(boolean isSleeping) {
		player.setSleepingIgnored(isSleeping);
	}

	@Override
	public boolean isSleepingIgnored() {
		return player.isSleepingIgnored();
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+), (\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5, $6 $7) { player.$1($3, $5, $7);"
	@Override
	public void playNote(Location loc, byte instrument, byte note) {
		player.playNote(loc, instrument, note);
	}

	@Override
	public void playNote(Location loc, Instrument instrument, Note note) {
		player.playNote(loc, instrument, note);
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5, $6 $7, $8 $9) { player.$1($3, $5, $7, $9);"
	@Override
	public void playSound(Location location, Sound sound, float volume, float pitch) {
		player.playSound(location, sound, volume, pitch);
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
		player.playSound(location, sound, volume, pitch);
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5, $6 $7, $8 $9, $10 $11) {
	// player.$1($3, $5, $7, $9, $11);"
	@Override
	public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
		player.playSound(location, sound, category, volume, pitch);
	}

	@Override
	public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
		player.playSound(location, sound, category, volume, pitch);
	}

	@Override
	public void stopSound(Sound sound) {
		player.stopSound(sound);
	}

	@Override
	public void stopSound(String sound) {
		player.stopSound(sound);
	}

	@Override
	public void stopSound(Sound sound, SoundCategory category) {
		player.stopSound(sound, category);
	}

	@Override
	public void stopSound(String sound, SoundCategory category) {
		player.stopSound(sound, category);
	}

	@Override
	public void playEffect(Location loc, Effect effect, int data) {
		player.playEffect(loc, effect, data);
	}

	@Override
	public <T> void playEffect(Location loc, Effect effect, T data) {
		player.playEffect(loc, effect, data);
	}

	@Override
	public void sendBlockChange(Location loc, Material material, byte data) {
		player.sendBlockChange(loc, material, data);
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5, $6 $7, $8 $9, $10 $11,
	// $12 $13) { player.$1($3, $5, $7, $9, $11, $13);"
	@Override
	public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void sendBlockChange(Location loc, int material, byte data) {
		player.sendBlockChange(loc, material, data);
	}

	@Override
	public void sendSignChange(Location loc, String[] lines) throws IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	@Override
	public void sendMap(MapView map) {
		player.sendMap(map);
	}

	// Find: " (\w+)\(\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1() { player.$1();"
	@Override
	public void updateInventory() {
		player.updateInventory();
	}

	@Override
	public void awardAchievement(Achievement achievement) {
		player.awardAchievement(achievement);
	}

	@Override
	public void removeAchievement(Achievement achievement) {
		player.removeAchievement(achievement);
	}

	@Override
	public boolean hasAchievement(Achievement achievement) {
		player.hasAchievement(achievement);
		return false;
	}

	@Override
	public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
		player.incrementStatistic(statistic);
	}

	@Override
	public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
		player.decrementStatistic(statistic);
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+)\)(.+)\{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5) $6 { player.$1($3, $5);"
	@Override
	public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
		player.incrementStatistic(statistic, amount);
	}

	@Override
	public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {
		player.decrementStatistic(statistic, amount);
	}

	@Override
	public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {
		player.setStatistic(statistic, newValue);
	}

	@Override
	public int getStatistic(Statistic statistic) throws IllegalArgumentException {
		player.getStatistic(statistic);
		return 0;
	}

	@Override
	public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
		player.incrementStatistic(statistic, material);
	}

	@Override
	public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
		player.decrementStatistic(statistic, material);
	}

	@Override
	public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {
		player.getStatistic(statistic, material);
		return 0;
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+), (\w+) (\w+)\)(.+)\{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5, $6 $7) $8 { player.$1($3, $5, $7);"
	@Override
	public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
		player.incrementStatistic(statistic, material, amount);
	}

	@Override
	public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {
		player.decrementStatistic(statistic, material, amount);
	}

	@Override
	public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {
		player.setStatistic(statistic, material, newValue);
	}

	@Override
	public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
		player.incrementStatistic(statistic, entityType);
	}

	@Override
	public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
		player.decrementStatistic(statistic, entityType);
	}

	@Override
	public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {
		player.getStatistic(statistic, entityType);
		return 0;
	}

	@Override
	public void incrementStatistic(Statistic statistic, EntityType entityType, int amount)
			throws IllegalArgumentException {
		// TODO Auto-generated method stub

	}

	@Override
	public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {
		player.decrementStatistic(statistic, entityType, amount);
	}

	@Override
	public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {
		player.setStatistic(statistic, entityType, newValue);
	}

	/**
	 * Find: " (\w+)\((\w+) (\w+), (\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5) { player.$1($3, $5);"
	 */
	@Override
	public void setPlayerTime(long time, boolean relative) {
		player.setPlayerTime(time, relative);
	}

	@Override
	public long getPlayerTime() {
		player.getPlayerTime();
		return 0;
	}

	@Override
	public long getPlayerTimeOffset() {
		player.getPlayerTimeOffset();
		return 0;
	}

	@Override
	public boolean isPlayerTimeRelative() {
		return player.isPlayerTimeRelative();
	}

	@Override
	public void resetPlayerTime() {
		player.resetPlayerTime();
	}

	@Override
	public void setPlayerWeather(WeatherType type) {
		player.setPlayerWeather(type);
	}

	@Override
	public WeatherType getPlayerWeather() {
		return player.getPlayerWeather();
	}

	@Override
	public void resetPlayerWeather() {
		player.resetPlayerWeather();
	}

	@Override
	public void giveExp(int amount) {
		player.giveExp(amount);
	}

	@Override
	public void giveExpLevels(int amount) {
		player.giveExpLevels(amount);
	}

	@Override
	public float getExp() {
		player.getExp();
		return 0;
	}

	@Override
	public void setExp(float exp) {
		player.setExp(exp);
	}

	@Override
	public int getLevel() {
		player.getLevel();
		return 0;
	}

	@Override
	public void setLevel(int level) {
		player.setLevel(level);
	}

	@Override
	public int getTotalExperience() {
		player.getTotalExperience();
		return 0;
	}

	@Override
	public void setTotalExperience(int exp) {
		player.setTotalExperience(exp);
	}

	@Override
	public float getExhaustion() {
		player.getExhaustion();
		return 0;
	}

	@Override
	public void setExhaustion(float value) {
		player.setExhaustion(value);
	}

	@Override
	public float getSaturation() {
		player.getSaturation();
		return 0;
	}

	@Override
	public void setSaturation(float value) {
		player.setSaturation(value);
	}

	@Override
	public int getFoodLevel() {
		player.getFoodLevel();
		return 0;
	}

	@Override
	public void setFoodLevel(int value) {
		player.setFoodLevel(value);
	}

	@Override
	public Location getBedSpawnLocation() {
		return player.getBedSpawnLocation();
	}

	@Override
	public void setBedSpawnLocation(Location location) {
		player.setBedSpawnLocation(location);
	}

	@Override
	public void setBedSpawnLocation(Location location, boolean force) {
		player.setBedSpawnLocation(location, force);
	}

	@Override
	public boolean getAllowFlight() {
		return player.getAllowFlight();
	}

	@Override
	public void setAllowFlight(boolean flight) {
		player.setAllowFlight(flight);
	}

	@Override
	public void hidePlayer(Player player) {
		player.hidePlayer(player);
	}

	@Override
	public void showPlayer(Player player) {
		player.showPlayer(player);
	}

	@Override
	public boolean canSee(Player player) {
		player.canSee(player);
		return false;
	}

	@Override
	public boolean isOnGround() {
		return player.isOnGround();
	}

	@Override
	public boolean isFlying() {
		return player.isFlying();
	}

	@Override
	public void setFlying(boolean value) {
		player.setFlying(value);
	}

	@Override
	public void setFlySpeed(float value) throws IllegalArgumentException {
		player.setFlySpeed(value);
	}

	@Override
	public void setWalkSpeed(float value) throws IllegalArgumentException {
		player.setWalkSpeed(value);
	}

	@Override
	public float getFlySpeed() {
		player.getFlySpeed();
		return 0;
	}

	@Override
	public float getWalkSpeed() {
		player.getWalkSpeed();
		return 0;
	}

	@Override
	public void setTexturePack(String url) {
		player.setTexturePack(url);
	}

	@Override
	public void setResourcePack(String url) {
		player.setResourcePack(url);
	}

	@Override
	public Scoreboard getScoreboard() {
		return player.getScoreboard();
	}

	@Override
	public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
		player.setScoreboard(scoreboard);
	}

	@Override
	public boolean isHealthScaled() {
		return player.isHealthScaled();
	}

	@Override
	public void setHealthScaled(boolean scale) {
		player.setHealthScaled(scale);
	}

	// Find: " (\w+)\((\w+) (\w+)\)(.+)\{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3) $4 { player.$1($3);"
	@Override
	public void setHealthScale(double scale) throws IllegalArgumentException {
		player.setHealthScale(scale);
	}

	@Override
	public double getHealthScale() {
		player.getHealthScale();
		return 0;
	}

	@Override
	public Entity getSpectatorTarget() {
		return player.getSpectatorTarget();
	}

	@Override
	public void setSpectatorTarget(Entity entity) {
		player.setSpectatorTarget(entity);
	}

	@Override
	public void sendTitle(String title, String subtitle) {
		player.sendTitle(title, subtitle);
	}

	@Override
	public void resetTitle() {
		player.resetTitle();
	}

	@Override
	public void spawnParticle(Particle particle, Location location, int count) {
		player.spawnParticle(particle, location, count);
	}

	@Override
	public void spawnParticle(Particle particle, double x, double y, double z, int count) {
		player.spawnParticle(particle, x, y, z, count);
	}

	@Override
	public <T> void spawnParticle(Particle particle, Location location, int count, T data) {
		player.spawnParticle(particle, location, count, data);
	}

	// Find: " (\w+)\((\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+), (\w+) (\w+)\) \{\s+\/\/ TO\DO Auto-generated method stub\s" - Replace: " $1($2 $3, $4 $5, $6 $7, $8 $9, $10 $11,
	// $12 $13) { player.$1($3, $5, $7, $9, $11, $13);"
	@Override
	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {
		player.spawnParticle(particle, x, y, z, count, data);
	}

	@Override
	public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ) {
		// TODO Auto-generated method stub

	}

	@Override
	public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ) {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, T data) {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, T data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, double extra) {
		// TODO Auto-generated method stub

	}

	@Override
	public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, double extra) {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, double extra, T data) {
		// TODO Auto-generated method stub

	}

	@Override
	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, double extra, T data) {
		// TODO Auto-generated method stub

	}

	@Override
	public Spigot spigot() {
		return player.spigot();
	}

	@Override
	public PlayerInventory getInventory() {
		return player.getInventory();
	}

	@Override
	public Inventory getEnderChest() {
		return player.getEnderChest();
	}

	@Override
	public MainHand getMainHand() {
		return player.getMainHand();
	}

	@Override
	public boolean setWindowProperty(Property prop, int value) {
		player.setWindowProperty(prop, value);
		return false;
	}

	@Override
	public InventoryView getOpenInventory() {
		return player.getOpenInventory();
	}

	@Override
	public InventoryView openInventory(Inventory inventory) {
		player.openInventory(inventory);
		return null;
	}

	@Override
	public InventoryView openWorkbench(Location location, boolean force) {
		player.openWorkbench(location, force);
		return null;
	}

	@Override
	public InventoryView openEnchanting(Location location, boolean force) {
		player.openEnchanting(location, force);
		return null;
	}

	@Override
	public void openInventory(InventoryView inventory) {
		player.openInventory(inventory);
	}

	@Override
	public InventoryView openMerchant(Villager trader, boolean force) {
		player.openMerchant(trader, force);
		return null;
	}

	@Override
	public InventoryView openMerchant(Merchant merchant, boolean force) {
		player.openMerchant(merchant, force);
		return null;
	}

	@Override
	public void closeInventory() {
		player.closeInventory();
	}

	@Override
	public ItemStack getItemInHand() {
		return player.getItemInHand();
	}

	@Override
	public void setItemInHand(ItemStack item) {
		player.setItemInHand(item);
	}

	@Override
	public ItemStack getItemOnCursor() {
		return player.getItemOnCursor();
	}

	@Override
	public void setItemOnCursor(ItemStack item) {
		player.setItemOnCursor(item);
	}

	@Override
	public boolean isSleeping() {
		return player.isSleeping();
	}

	@Override
	public int getSleepTicks() {
		player.getSleepTicks();
		return 0;
	}

	@Override
	public GameMode getGameMode() {
		return player.getGameMode();
	}

	@Override
	public void setGameMode(GameMode mode) {
		player.setGameMode(mode);
	}

	@Override
	public boolean isBlocking() {
		return player.isBlocking();
	}

	@Override
	public boolean isHandRaised() {
		return player.isHandRaised();
	}

	@Override
	public int getExpToLevel() {
		player.getExpToLevel();
		return 0;
	}

	@Override
	public double getEyeHeight() {
		player.getEyeHeight();
		return 0;
	}

	@Override
	public double getEyeHeight(boolean ignoreSneaking) {
		player.getEyeHeight(ignoreSneaking);
		return 0;
	}

	@Override
	public Location getEyeLocation() {
		return player.getEyeLocation();
	}

	@Override
	public List<Block> getLineOfSight(HashSet<Byte> transparent, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getLineOfSight(Set<Material> transparent, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Block getTargetBlock(HashSet<Byte> transparent, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Block getTargetBlock(Set<Material> transparent, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getLastTwoTargetBlocks(HashSet<Byte> transparent, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Block> getLastTwoTargetBlocks(Set<Material> transparent, int maxDistance) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getRemainingAir() {
		player.getRemainingAir();
		return 0;
	}

	@Override
	public void setRemainingAir(int ticks) {
		player.setRemainingAir(ticks);
	}

	@Override
	public int getMaximumAir() {
		player.getMaximumAir();
		return 0;
	}

	@Override
	public void setMaximumAir(int ticks) {
		player.setMaximumAir(ticks);
	}

	@Override
	public int getMaximumNoDamageTicks() {
		player.getMaximumNoDamageTicks();
		return 0;
	}

	@Override
	public void setMaximumNoDamageTicks(int ticks) {
		player.setMaximumNoDamageTicks(ticks);
	}

	@Override
	public double getLastDamage() {
		player.getLastDamage();
		return 0;
	}

	@Override
	public int _INVALID_getLastDamage() {
		player._INVALID_getLastDamage();
		return 0;
	}

	@Override
	public void setLastDamage(double damage) {
		player.setLastDamage(damage);
	}

	@Override
	public void _INVALID_setLastDamage(int damage) {
		player._INVALID_setLastDamage(damage);
	}

	@Override
	public int getNoDamageTicks() {
		player.getNoDamageTicks();
		return 0;
	}

	@Override
	public void setNoDamageTicks(int ticks) {
		player.setNoDamageTicks(ticks);
	}

	@Override
	public Player getKiller() {
		return player.getKiller();
	}

	@Override
	public boolean addPotionEffect(PotionEffect effect) {
		player.addPotionEffect(effect);
		return false;
	}

	@Override
	public boolean addPotionEffect(PotionEffect effect, boolean force) {
		player.addPotionEffect(effect, force);
		return false;
	}

	@Override
	public boolean addPotionEffects(Collection<PotionEffect> effects) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasPotionEffect(PotionEffectType type) {
		player.hasPotionEffect(type);
		return false;
	}

	@Override
	public PotionEffect getPotionEffect(PotionEffectType type) {
		player.getPotionEffect(type);
		return null;
	}

	@Override
	public void removePotionEffect(PotionEffectType type) {
		player.removePotionEffect(type);
	}

	@Override
	public Collection<PotionEffect> getActivePotionEffects() {
		return player.getActivePotionEffects();
	}

	@Override
	public boolean hasLineOfSight(Entity other) {
		player.hasLineOfSight(other);
		return false;
	}

	@Override
	public boolean getRemoveWhenFarAway() {
		return player.getRemoveWhenFarAway();
	}

	@Override
	public void setRemoveWhenFarAway(boolean remove) {
		player.setRemoveWhenFarAway(remove);
	}

	@Override
	public EntityEquipment getEquipment() {
		return player.getEquipment();
	}

	@Override
	public void setCanPickupItems(boolean pickup) {
		player.setCanPickupItems(pickup);
	}

	@Override
	public boolean getCanPickupItems() {
		return player.getCanPickupItems();
	}

	@Override
	public boolean isLeashed() {
		return player.isLeashed();
	}

	@Override
	public Entity getLeashHolder() throws IllegalStateException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean setLeashHolder(Entity holder) {
		player.setLeashHolder(holder);
		return false;
	}

	@Override
	public boolean isGliding() {
		return player.isGliding();
	}

	@Override
	public void setGliding(boolean gliding) {
		player.setGliding(gliding);
	}

	@Override
	public void setAI(boolean ai) {
		player.setAI(ai);
	}

	@Override
	public boolean hasAI() {
		return player.hasAI();
	}

	@Override
	public void setCollidable(boolean collidable) {
		player.setCollidable(collidable);
	}

	@Override
	public boolean isCollidable() {
		return player.isCollidable();
	}

	@Override
	public AttributeInstance getAttribute(Attribute attribute) {
		player.getAttribute(attribute);
		return null;
	}

	@Override
	public Location getLocation() {
		return player.getLocation();
	}

	@Override
	public Location getLocation(Location loc) {
		player.getLocation(loc);
		return null;
	}

	@Override
	public void setVelocity(Vector velocity) {
		player.setVelocity(velocity);
	}

	@Override
	public Vector getVelocity() {
		return player.getVelocity();
	}

	@Override
	public World getWorld() {
		return player.getWorld();
	}

	@Override
	public boolean teleport(Location location) {
		player.teleport(location);
		return false;
	}

	@Override
	public boolean teleport(Location location, TeleportCause cause) {
		player.teleport(location, cause);
		return false;
	}

	@Override
	public boolean teleport(Entity destination) {
		player.teleport(destination);
		return false;
	}

	@Override
	public boolean teleport(Entity destination, TeleportCause cause) {
		player.teleport(destination, cause);
		return false;
	}

	@Override
	public List<Entity> getNearbyEntities(double x, double y, double z) {
		player.getNearbyEntities(x, y, z);
		return null;
	}

	@Override
	public int getEntityId() {
		player.getEntityId();
		return 0;
	}

	@Override
	public int getFireTicks() {
		player.getFireTicks();
		return 0;
	}

	@Override
	public int getMaxFireTicks() {
		player.getMaxFireTicks();
		return 0;
	}

	@Override
	public void setFireTicks(int ticks) {
		player.setFireTicks(ticks);
	}

	@Override
	public void remove() {
		player.remove();
	}

	@Override
	public boolean isDead() {
		return player.isDead();
	}

	@Override
	public boolean isValid() {
		return player.isValid();
	}

	@Override
	public Entity getPassenger() {
		return player.getPassenger();
	}

	@Override
	public boolean setPassenger(Entity passenger) {
		player.setPassenger(passenger);
		return false;
	}

	@Override
	public boolean isEmpty() {
		return player.isEmpty();
	}

	@Override
	public boolean eject() {
		return player.eject();
	}

	@Override
	public float getFallDistance() {
		player.getFallDistance();
		return 0;
	}

	@Override
	public void setFallDistance(float distance) {
		player.setFallDistance(distance);
	}

	@Override
	public void setLastDamageCause(EntityDamageEvent event) {
		player.setLastDamageCause(event);
	}

	@Override
	public EntityDamageEvent getLastDamageCause() {
		return player.getLastDamageCause();
	}

	@Override
	public UUID getUniqueId() {
		return player.getUniqueId();
	}

	@Override
	public int getTicksLived() {
		player.getTicksLived();
		return 0;
	}

	@Override
	public void setTicksLived(int value) {
		player.setTicksLived(value);
	}

	@Override
	public void playEffect(EntityEffect type) {
		player.playEffect(type);
	}

	@Override
	public EntityType getType() {
		return player.getType();
	}

	@Override
	public boolean isInsideVehicle() {
		return player.isInsideVehicle();
	}

	@Override
	public boolean leaveVehicle() {
		return player.leaveVehicle();
	}

	@Override
	public Entity getVehicle() {
		return player.getVehicle();
	}

	@Override
	public void setCustomNameVisible(boolean flag) {
		player.setCustomNameVisible(flag);
	}

	@Override
	public boolean isCustomNameVisible() {
		return player.isCustomNameVisible();
	}

	@Override
	public void setGlowing(boolean flag) {
		player.setGlowing(flag);
	}

	@Override
	public boolean isGlowing() {
		return player.isGlowing();
	}

	@Override
	public void setInvulnerable(boolean flag) {
		player.setInvulnerable(flag);
	}

	@Override
	public boolean isInvulnerable() {
		return player.isInvulnerable();
	}

	@Override
	public boolean isSilent() {
		return player.isSilent();
	}

	@Override
	public void setSilent(boolean flag) {
		player.setSilent(flag);
	}

	@Override
	public boolean hasGravity() {
		return player.hasGravity();
	}

	@Override
	public void setGravity(boolean gravity) {
		player.setGravity(gravity);
	}

	@Override
	public int getPortalCooldown() {
		player.getPortalCooldown();
		return 0;
	}

	@Override
	public void setPortalCooldown(int cooldown) {
		player.setPortalCooldown(cooldown);
	}

	@Override
	public Set<String> getScoreboardTags() {
		return player.getScoreboardTags();
	}

	@Override
	public boolean addScoreboardTag(String tag) {
		player.addScoreboardTag(tag);
		return false;
	}

	@Override
	public boolean removeScoreboardTag(String tag) {
		player.removeScoreboardTag(tag);
		return false;
	}

	@Override
	public void setMetadata(String metadataKey, MetadataValue newMetadataValue) {
		player.setMetadata(metadataKey, newMetadataValue);
	}

	@Override
	public List<MetadataValue> getMetadata(String metadataKey) {
		player.getMetadata(metadataKey);
		return null;
	}

	@Override
	public boolean hasMetadata(String metadataKey) {
		player.hasMetadata(metadataKey);
		return false;
	}

	@Override
	public void removeMetadata(String metadataKey, Plugin owningPlugin) {
		player.removeMetadata(metadataKey, owningPlugin);
	}

	@Override
	public String getCustomName() {
		return player.getCustomName();
	}

	@Override
	public void setCustomName(String name) {
		player.setCustomName(name);
	}

	@Override
	public void damage(double amount) {
		player.damage(amount);
	}

	@Override
	public void _INVALID_damage(int amount) {
		player._INVALID_damage(amount);
	}

	@Override
	public void damage(double amount, Entity source) {
		player.damage(amount, source);
	}

	@Override
	public void _INVALID_damage(int amount, Entity source) {
		player._INVALID_damage(amount, source);
	}

	@Override
	public double getHealth() {
		player.getHealth();
		return 0;
	}

	@Override
	public int _INVALID_getHealth() {
		player._INVALID_getHealth();
		return 0;
	}

	@Override
	public void setHealth(double health) {
		player.setHealth(health);
	}

	@Override
	public void _INVALID_setHealth(int health) {
		player._INVALID_setHealth(health);
	}

	@Override
	public double getMaxHealth() {
		player.getMaxHealth();
		return 0;
	}

	@Override
	public int _INVALID_getMaxHealth() {
		player._INVALID_getMaxHealth();
		return 0;
	}

	@Override
	public void setMaxHealth(double health) {
		player.setMaxHealth(health);
	}

	@Override
	public void _INVALID_setMaxHealth(int health) {
		player._INVALID_setMaxHealth(health);
	}

	@Override
	public void resetMaxHealth() {
		player.resetMaxHealth();
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConversing() {
		return player.isConversing();
	}

	@Override
	public void acceptConversationInput(String input) {
		player.acceptConversationInput(input);
	}

	@Override
	public boolean beginConversation(Conversation conversation) {
		player.beginConversation(conversation);
		return false;
	}

	@Override
	public void abandonConversation(Conversation conversation) {
		player.abandonConversation(conversation);
	}

	@Override
	public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
		player.abandonConversation(conversation, details);
	}

	@Override
	public boolean isOnline() {
		return player.isOnline();
	}

	@Override
	public boolean isBanned() {
		return player.isBanned();
	}

	@Override
	public void setBanned(boolean banned) {
		player.setBanned(banned);
	}

	@Override
	public boolean isWhitelisted() {
		return player.isWhitelisted();
	}

	@Override
	public void setWhitelisted(boolean value) {
		player.setWhitelisted(value);
	}

	@Override
	public Player getPlayer() {
		return player.getPlayer();
	}

	@Override
	public long getFirstPlayed() {
		player.getFirstPlayed();
		return 0;
	}

	@Override
	public long getLastPlayed() {
		player.getLastPlayed();
		return 0;
	}

	@Override
	public boolean hasPlayedBefore() {
		return player.hasPlayedBefore();
	}

	@Override
	public Map<String, Object> serialize() {
		return player.serialize();
	}

	@Override
	public void sendPluginMessage(Plugin source, String channel, byte[] message) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<String> getListeningPluginChannels() {
		return player.getListeningPluginChannels();
	}
}
