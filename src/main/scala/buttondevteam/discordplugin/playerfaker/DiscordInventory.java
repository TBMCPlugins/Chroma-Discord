package buttondevteam.discordplugin.playerfaker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.IntStream;

public class DiscordInventory implements Inventory {
	private ItemStack[] items = new ItemStack[27];
	private List<ItemStack> itemStacks = Arrays.asList(items);

	public int maxStackSize;
	private static ItemStack emptyStack = new ItemStack(Material.AIR, 0);

	@Override
	public int getSize() {
		return items.length;
	}

	@Override
	public int getMaxStackSize() {
		return maxStackSize;
	}

	@Override
	public void setMaxStackSize(int maxStackSize) {
		this.maxStackSize = maxStackSize;
	}

	@Override
	public String getName() {
		return "Discord inventory";
	}

	@Override
	public ItemStack getItem(int index) {
		if (index >= items.length)
			return emptyStack;
		else
			return items[index];
	}

	@Override
	public void setItem(int index, ItemStack item) {
		if (index < items.length)
			items[index] = item;
	}

	@Override
	public HashMap<Integer, ItemStack> addItem(ItemStack... items) throws IllegalArgumentException {
		return IntStream.range(0, items.length).collect(HashMap::new, (map, i) -> map.put(i, items[i]), HashMap::putAll); //Pretend that we can't add anything
	}

	@Override
	public HashMap<Integer, ItemStack> removeItem(ItemStack... items) throws IllegalArgumentException {
		return IntStream.range(0, items.length).collect(HashMap::new, (map, i) -> map.put(i, items[i]), HashMap::putAll); //Pretend that we can't add anything
	}

	@Override
	public ItemStack[] getContents() {
		return items;
	}

	@Override
	public void setContents(ItemStack[] items) throws IllegalArgumentException {
		this.items = items;
	}

	@Override
	public ItemStack[] getStorageContents() {
		return items;
	}

	@Override
	public void setStorageContents(ItemStack[] items) throws IllegalArgumentException {
		this.items = items;
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean contains(int materialId) {
		return itemStacks.stream().anyMatch(is -> is.getType().getId() == materialId);
	}

	@Override
	public boolean contains(Material material) throws IllegalArgumentException {
		return itemStacks.stream().anyMatch(is -> is.getType() == material);
	}

	@Override
	public boolean contains(ItemStack item) {
		return itemStacks.stream().anyMatch(is -> is.getType() == item.getType() && is.getAmount() == item.getAmount());
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean contains(int materialId, int amount) {
		return itemStacks.stream().anyMatch(is -> is.getType().getId() == materialId && is.getAmount() == amount);
	}

	@Override
	public boolean contains(Material material, int amount) throws IllegalArgumentException {
		return itemStacks.stream().anyMatch(is -> is.getType() == material && is.getAmount() == amount);
	}

	@Override
	public boolean contains(ItemStack item, int amount) { //Not correct implementation but whatever
		return itemStacks.stream().anyMatch(is -> is.getType() == item.getType() && is.getAmount() == amount);
	}

	@Override
	public boolean containsAtLeast(ItemStack item, int amount) {
		return false;
	}

	@Override
	@Deprecated
	public HashMap<Integer, ? extends ItemStack> all(int materialId) {
		return new HashMap<>();
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(Material material) throws IllegalArgumentException {
		return new HashMap<>();
	}

	@Override
	public HashMap<Integer, ? extends ItemStack> all(ItemStack item) {
		return new HashMap<>();
	}

	@Override
	@Deprecated
	public int first(int materialId) {
		return -1;
	}

	@Override
	public int first(Material material) throws IllegalArgumentException {
		return -1;
	}

	@Override
	public int first(ItemStack item) {
		return -1;
	}

	@Override
	public int firstEmpty() {
		return -1;
	}

	@Override
	@Deprecated
	public void remove(int materialId) {
	}

	@Override
	public void remove(Material material) throws IllegalArgumentException {
	}

	@Override
	public void remove(ItemStack item) {
	}

	@Override
	public void clear(int index) {
		if (index < items.length)
			items[index] = null;
	}

	@Override
	public void clear() {
		Arrays.fill(items, null);
	}

	@Override
	public List<HumanEntity> getViewers() {
		return Collections.emptyList();
	}

	@Override
	public String getTitle() {
		return "Discord inventory";
	}

	@Override
	public InventoryType getType() {
		return InventoryType.CHEST;
	}

	@Override
	public InventoryHolder getHolder() {
		return null;
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public ListIterator<ItemStack> iterator() {
		return itemStacks.listIterator();
	}

	@Override
	public ListIterator<ItemStack> iterator(int index) {
		return itemStacks.listIterator(index);
	}

	@Override
	public Location getLocation() {
		return null;
	}
}
