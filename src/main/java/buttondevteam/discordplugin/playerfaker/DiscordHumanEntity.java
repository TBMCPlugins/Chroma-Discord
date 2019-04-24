package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.User;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.*;
import org.bukkit.inventory.InventoryView.Property;

import java.util.UUID;

public abstract class DiscordHumanEntity extends DiscordLivingEntity implements HumanEntity {
	protected DiscordHumanEntity(User user, MessageChannel channel, int entityId, UUID uuid, MinecraftChatModule module) {
		super(user, channel, entityId, uuid, module);
	}

	private PlayerInventory inv = new DiscordPlayerInventory(this);

	@Override
	public PlayerInventory getInventory() {
		return inv;
	}

	private Inventory enderchest = new DiscordInventory(this);

	@Override
	public Inventory getEnderChest() {
		return enderchest;
	}

	@Override
	public MainHand getMainHand() {
		return MainHand.RIGHT;
	}

	@Override
	public boolean setWindowProperty(Property prop, int value) {
		return false;
	}

	@Override
	public InventoryView getOpenInventory() { // TODO: Test
		return null;
	}

	@Override
	public InventoryView openInventory(Inventory inventory) {
		return null;
	}

	@Override
	public InventoryView openWorkbench(Location location, boolean force) {
		return null;
	}

	@Override
	public InventoryView openEnchanting(Location location, boolean force) {
		return null;
	}

	@Override
	public void openInventory(InventoryView inventory) {
	}

	@Override
	public InventoryView openMerchant(Villager trader, boolean force) {
		return null;
	}

	@Override
	public InventoryView openMerchant(Merchant merchant, boolean force) {
		return null;
	}

	@Override
	public void closeInventory() {
	}

	@Override
	public ItemStack getItemInHand() { // TODO: Test all ItemStack methods
		return null;
	}

	@Override
	public void setItemInHand(ItemStack item) {
	}

	@Override
	public ItemStack getItemOnCursor() {
		return null;
	}

	@Override
	public void setItemOnCursor(ItemStack item) {
	}

	@Override
	public boolean hasCooldown(Material material) {
		return false;
	}

	@Override
	public int getCooldown(Material material) {
		return 0;
	}

	@Override
	public void setCooldown(Material material, int ticks) {
	}

	@Override
	public boolean isSleeping() {
		return false;
	}

	@Override
	public int getSleepTicks() {
		return 0;
	}

	@Override
	public GameMode getGameMode() {
		return GameMode.SPECTATOR;
	}

	@Override
	public void setGameMode(GameMode mode) {
	}

	@Override
	public boolean isBlocking() {
		return false;
	}

	@Override
	public boolean isHandRaised() {
		return false;
	}

	@Override
	public int getExpToLevel() {
		return 0;
	}

	@Override
	public Entity getShoulderEntityLeft() {
		return null;
	}

	@Override
	public void setShoulderEntityLeft(Entity entity) {
	}

	@Override
	public Entity getShoulderEntityRight() {
		return null;
	}

	@Override
	public void setShoulderEntityRight(Entity entity) {
	}

}
