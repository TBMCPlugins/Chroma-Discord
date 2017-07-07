package buttondevteam.discordplugin.playerfaker;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class DiscordPlayerInventory extends DiscordInventory implements PlayerInventory {
	public DiscordPlayerInventory(DiscordHumanEntity holder) {
		super(holder);
	}

	@Override
	public ItemStack[] getArmorContents() {
		return new ItemStack[0];
	}

	@Override
	public ItemStack[] getExtraContents() {
		return new ItemStack[0];
	}

	@Override
	public ItemStack getHelmet() {
		return null;
	}

	@Override
	public ItemStack getChestplate() {
		return null;
	}

	@Override
	public ItemStack getLeggings() {
		return null;
	}

	@Override
	public ItemStack getBoots() {
		return null;
	}

	@Override
	public void setArmorContents(ItemStack[] items) {
	}

	@Override
	public void setExtraContents(ItemStack[] items) {
	}

	@Override
	public void setHelmet(ItemStack helmet) {
	}

	@Override
	public void setChestplate(ItemStack chestplate) {
	}

	@Override
	public void setLeggings(ItemStack leggings) {
	}

	@Override
	public void setBoots(ItemStack boots) {
	}

	@Override
	public ItemStack getItemInMainHand() {
		return null;
	}

	@Override
	public void setItemInMainHand(ItemStack item) {
	}

	@Override
	public ItemStack getItemInOffHand() {
		return null;
	}

	@Override
	public void setItemInOffHand(ItemStack item) {
	}

	@Override
	public ItemStack getItemInHand() {
		return null;
	}

	@Override
	public void setItemInHand(ItemStack stack) {
	}

	@Override
	public int getHeldItemSlot() {
		return 0;
	}

	@Override
	public void setHeldItemSlot(int slot) {
	}

	@Override
	public int clear(int id, int data) {
		return 0;
	}
}
