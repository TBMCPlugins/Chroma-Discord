package buttondevteam.discordplugin.playerfaker;

import java.util.UUID;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.InventoryView.Property;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.PlayerInventory;

import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public abstract class DiscordHumanEntity extends DiscordLivingEntity implements HumanEntity {
	protected DiscordHumanEntity(IUser user, IChannel channel, int entityId, UUID uuid) {
		super(user, channel, entityId, uuid);
	}

	@Override
	public PlayerInventory getInventory() { // TODO
		return null;
	}

	@Override
	public Inventory getEnderChest() {
		// TODO Auto-generated method stub
		return null;
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
	public InventoryView getOpenInventory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InventoryView openInventory(Inventory inventory) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InventoryView openWorkbench(Location location, boolean force) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InventoryView openEnchanting(Location location, boolean force) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void openInventory(InventoryView inventory) {
		// TODO Auto-generated method stub

	}

	@Override
	public InventoryView openMerchant(Villager trader, boolean force) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InventoryView openMerchant(Merchant merchant, boolean force) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void closeInventory() {
		// TODO Auto-generated method stub

	}

	@Override
	public ItemStack getItemInHand() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setItemInHand(ItemStack item) {
		// TODO Auto-generated method stub

	}

	@Override
	public ItemStack getItemOnCursor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setItemOnCursor(ItemStack item) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean hasCooldown(Material material) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getCooldown(Material material) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setCooldown(Material material, int ticks) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSleeping() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getSleepTicks() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public GameMode getGameMode() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setGameMode(GameMode mode) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isBlocking() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHandRaised() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getExpToLevel() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Entity getShoulderEntityLeft() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setShoulderEntityLeft(Entity entity) {
		// TODO Auto-generated method stub

	}

	@Override
	public Entity getShoulderEntityRight() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setShoulderEntityRight(Entity entity) {
		// TODO Auto-generated method stub

	}

}
