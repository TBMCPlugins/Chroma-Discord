package buttondevteam.discordplugin.playerfaker;

import java.net.InetSocketAddress;
import java.util.*;
import org.bukkit.*;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.map.MapView;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;

import buttondevteam.discordplugin.DiscordPlugin;
import lombok.experimental.Delegate;
import lombok.Getter;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class DiscordFakePlayer extends DiscordHumanEntity implements Player {
	protected DiscordFakePlayer(IUser user, IChannel channel, int entityId, UUID uuid, String mcname) {
		super(user, channel, entityId, uuid);
		perm = new PermissibleBase(Bukkit.getOfflinePlayer(uuid));
		name = mcname;
	}

	@Delegate
	private PermissibleBase perm;

	private @Getter String name;

	@Override
	public EntityType getType() {
		return EntityType.PLAYER;
	}

	@Override
	public String getCustomName() {
		return user.getName();
	}

	@Override
	public void setCustomName(String name) {
	}

	@Override
	public boolean isConversing() {

		return false;
	}

	@Override
	public void acceptConversationInput(String input) {
	}

	@Override
	public boolean beginConversation(Conversation conversation) {
		return false;
	}

	@Override
	public void abandonConversation(Conversation conversation) {
	}

	@Override
	public void abandonConversation(Conversation conversation, ConversationAbandonedEvent details) {
	}

	@Override
	public boolean isOnline() {
		return true;// Let's pretend
	}

	@Override
	public boolean isBanned() {
		return false;
	}

	@Override
	public boolean isWhitelisted() {
		return true;
	}

	@Override
	public void setWhitelisted(boolean value) {
	}

	@Override
	public Player getPlayer() {
		return this;
	}

	@Override
	public long getFirstPlayed() {
		return 0;
	}

	@Override
	public long getLastPlayed() {
		return 0;
	}

	@Override
	public boolean hasPlayedBefore() {
		return false;
	}

	@Override
	public Map<String, Object> serialize() {
		return new HashMap<>();
	}

	@Override
	public void sendPluginMessage(Plugin source, String channel, byte[] message) {
	}

	@Override
	public Set<String> getListeningPluginChannels() {
		return Collections.emptySet();
	}

	@Override
	public String getDisplayName() {
		return user.getDisplayName(DiscordPlugin.mainServer);
	}

	@Override
	public void setDisplayName(String name) {
	}

	@Override
	public String getPlayerListName() {
		return getName();
	}

	@Override
	public void setPlayerListName(String name) {
	}

	@Override
	public void setCompassTarget(Location loc) {
	}

	@Override
	public Location getCompassTarget() {
		return new Location(Bukkit.getWorlds().get(0), 0, 0, 0);
	}

	@Override
	public InetSocketAddress getAddress() {
		return null;
	}

	@Override
	public void sendRawMessage(String message) {
		sendMessage(message);
	}

	@Override
	public void kickPlayer(String message) {
	}

	@Override
	public void chat(String msg) {
		Bukkit.getPluginManager()
				.callEvent(new AsyncPlayerChatEvent(true, this, msg, new HashSet<>(Bukkit.getOnlinePlayers())));
	}

	@Override
	public boolean performCommand(String command) {
		return Bukkit.getServer().dispatchCommand(this, command);
	}

	@Override
	public boolean isSneaking() {
		return false;
	}

	@Override
	public void setSneaking(boolean sneak) {
	}

	@Override
	public boolean isSprinting() {
		return false;
	}

	@Override
	public void setSprinting(boolean sprinting) {
	}

	@Override
	public void saveData() {
	}

	@Override
	public void loadData() {
	}

	@Override
	public void setSleepingIgnored(boolean isSleeping) {
	}

	@Override
	public boolean isSleepingIgnored() {
		return false;
	}

	@Override
	public void playNote(Location loc, byte instrument, byte note) {
	}

	@Override
	public void playNote(Location loc, Instrument instrument, Note note) {
	}

	@Override
	public void playSound(Location location, Sound sound, float volume, float pitch) {
	}

	@Override
	public void playSound(Location location, String sound, float volume, float pitch) {
	}

	@Override
	public void playSound(Location location, Sound sound, SoundCategory category, float volume, float pitch) {
	}

	@Override
	public void playSound(Location location, String sound, SoundCategory category, float volume, float pitch) {
	}

	@Override
	public void stopSound(Sound sound) {
	}

	@Override
	public void stopSound(String sound) {
	}

	@Override
	public void stopSound(Sound sound, SoundCategory category) {
	}

	@Override
	public void stopSound(String sound, SoundCategory category) {
	}

	@Override
	public void playEffect(Location loc, Effect effect, int data) {
	}

	@Override
	public <T> void playEffect(Location loc, Effect effect, T data) {
	}

	@Override
	public void sendBlockChange(Location loc, Material material, byte data) {
	}

	@Override
	public boolean sendChunkChange(Location loc, int sx, int sy, int sz, byte[] data) {
		return false;
	}

	@Override
	public void sendBlockChange(Location loc, int material, byte data) {
	}

	@Override
	public void sendSignChange(Location loc, String[] lines) throws IllegalArgumentException {
	}

	@Override
	public void sendMap(MapView map) {
	}

	@Override
	public void updateInventory() {
	}

	@Override
	public void awardAchievement(@SuppressWarnings("deprecation") Achievement achievement) {
	}

	@Override
	public void removeAchievement(@SuppressWarnings("deprecation") Achievement achievement) {
	}

	@Override
	public boolean hasAchievement(@SuppressWarnings("deprecation") Achievement achievement) {
		return false;
	}

	@Override
	public void incrementStatistic(Statistic statistic) throws IllegalArgumentException {
	}

	@Override
	public void decrementStatistic(Statistic statistic) throws IllegalArgumentException {
	}

	@Override
	public void incrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(Statistic statistic, int amount) throws IllegalArgumentException {

	}

	@Override
	public void setStatistic(Statistic statistic, int newValue) throws IllegalArgumentException {

	}

	@Override
	public int getStatistic(Statistic statistic) throws IllegalArgumentException {

		return 0;
	}

	@Override
	public void incrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(Statistic statistic, Material material) throws IllegalArgumentException {

	}

	@Override
	public int getStatistic(Statistic statistic, Material material) throws IllegalArgumentException {

		return 0;
	}

	@Override
	public void incrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(Statistic statistic, Material material, int amount) throws IllegalArgumentException {

	}

	@Override
	public void setStatistic(Statistic statistic, Material material, int newValue) throws IllegalArgumentException {

	}

	@Override
	public void incrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {

	}

	@Override
	public int getStatistic(Statistic statistic, EntityType entityType) throws IllegalArgumentException {

		return 0;
	}

	@Override
	public void incrementStatistic(Statistic statistic, EntityType entityType, int amount)
			throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(Statistic statistic, EntityType entityType, int amount) {

	}

	@Override
	public void setStatistic(Statistic statistic, EntityType entityType, int newValue) {

	}

	@Override
	public void setPlayerTime(long time, boolean relative) {

	}

	@Override
	public long getPlayerTime() {

		return 0;
	}

	@Override
	public long getPlayerTimeOffset() {

		return 0;
	}

	@Override
	public boolean isPlayerTimeRelative() {

		return false;
	}

	@Override
	public void resetPlayerTime() {

	}

	@Override
	public void setPlayerWeather(WeatherType type) {

	}

	@Override
	public WeatherType getPlayerWeather() {

		return null;
	}

	@Override
	public void resetPlayerWeather() {

	}

	@Override
	public void giveExp(int amount) {

	}

	@Override
	public void giveExpLevels(int amount) {

	}

	@Override
	public float getExp() {

		return 0;
	}

	@Override
	public void setExp(float exp) {

	}

	@Override
	public int getLevel() {

		return 0;
	}

	@Override
	public void setLevel(int level) {

	}

	@Override
	public int getTotalExperience() {

		return 0;
	}

	@Override
	public void setTotalExperience(int exp) {

	}

	@Override
	public float getExhaustion() {

		return 0;
	}

	@Override
	public void setExhaustion(float value) {

	}

	@Override
	public float getSaturation() {

		return 0;
	}

	@Override
	public void setSaturation(float value) {

	}

	@Override
	public int getFoodLevel() {

		return 0;
	}

	@Override
	public void setFoodLevel(int value) {

	}

	@Override
	public Location getBedSpawnLocation() {
		return null;
	}

	@Override
	public void setBedSpawnLocation(Location location) {
	}

	@Override
	public void setBedSpawnLocation(Location location, boolean force) {
	}

	@Override
	public boolean getAllowFlight() {
		return false;
	}

	@Override
	public void setAllowFlight(boolean flight) {
	}

	@Override
	public void hidePlayer(Player player) {
	}

	@Override
	public void showPlayer(Player player) {
	}

	@Override
	public boolean canSee(Player player) { // Nobody can see them
		return false;
	}

	@Override
	public boolean isFlying() {
		return false;
	}

	@Override
	public void setFlying(boolean value) {
	}

	@Override
	public void setFlySpeed(float value) throws IllegalArgumentException {
	}

	@Override
	public void setWalkSpeed(float value) throws IllegalArgumentException {
	}

	@Override
	public float getFlySpeed() {
		return 0;
	}

	@Override
	public float getWalkSpeed() {
		return 0;
	}

	@Override
	public void setTexturePack(String url) {
	}

	@Override
	public void setResourcePack(String url) {
	}

	@Override
	public void setResourcePack(String url, byte[] hash) {
	}

	@Override
	public Scoreboard getScoreboard() {
		return null;
	}

	@Override
	public void setScoreboard(Scoreboard scoreboard) throws IllegalArgumentException, IllegalStateException {
	}

	@Override
	public boolean isHealthScaled() {
		return false;
	}

	@Override
	public void setHealthScaled(boolean scale) {
	}

	@Override
	public void setHealthScale(double scale) throws IllegalArgumentException {
	}

	@Override
	public double getHealthScale() {
		return 1;
	}

	@Override
	public Entity getSpectatorTarget() {
		return null;
	}

	@Override
	public void setSpectatorTarget(Entity entity) {
	}

	@Override
	public void sendTitle(String title, String subtitle) {
	}

	@Override
	public void sendTitle(String title, String subtitle, int fadeIn, int stay, int fadeOut) {
	}

	@Override
	public void resetTitle() {
	}

	@Override
	public void spawnParticle(Particle particle, Location location, int count) {
	}

	@Override
	public void spawnParticle(Particle particle, double x, double y, double z, int count) {

	}

	@Override
	public <T> void spawnParticle(Particle particle, Location location, int count, T data) {

	}

	@Override
	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, T data) {

	}

	@Override
	public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ) {

	}

	@Override
	public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ) {

	}

	@Override
	public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, T data) {

	}

	@Override
	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, T data) {

	}

	@Override
	public void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, double extra) {

	}

	@Override
	public void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, double extra) {

	}

	@Override
	public <T> void spawnParticle(Particle particle, Location location, int count, double offsetX, double offsetY,
			double offsetZ, double extra, T data) {

	}

	@Override
	public <T> void spawnParticle(Particle particle, double x, double y, double z, int count, double offsetX,
			double offsetY, double offsetZ, double extra, T data) {

	}

	@Override
	public AdvancementProgress getAdvancementProgress(Advancement advancement) { // TODO: Test
		return null;
	}

	@Override
	public String getLocale() {

		return null;
	}

	@Override
	public Player.Spigot spigot() {
		return new Player.Spigot();
	}
}
