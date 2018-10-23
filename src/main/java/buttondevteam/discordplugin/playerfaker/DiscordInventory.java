package buttondevteam.discordplugin.playerfaker;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DiscordInventory implements Inventory {
	public DiscordInventory(DiscordHumanEntity holder) {
		this.holder = holder;
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public int getMaxStackSize() {
		return 0;
	}

	@Override
	public void setMaxStackSize(int size) {
	}

	@Override
	public String getName() {
		return "Player inventory";
	}

	@Override
	public ItemStack getItem(int index) {
		return null;
	}

	@Override
	public HashMap<Integer, ItemStack> addItem(ItemStack... items) throws IllegalArgumentException { // Can't add anything
		return new HashMap<>(
				IntStream.range(0, items.length).boxed().collect(Collectors.toMap(i -> i, i -> items[i])));
	}

	@Override
	public HashMap<Integer, ItemStack> removeItem(ItemStack... items) throws IllegalArgumentException {
		return new HashMap<>(
				IntStream.range(0, items.length).boxed().collect(Collectors.toMap(i -> i, i -> items[i])));
	}

	@Override
	public ItemStack[] getContents() {
		return new ItemStack[0];
	}

	@Override
	public void setContents(ItemStack[] items) throws IllegalArgumentException {
		if (items.length > 0)
			throw new IllegalArgumentException("This inventory does not support items");
	}

	@Override
	public ItemStack[] getStorageContents() {
		return new ItemStack[0];
	}

	@Override
	public void setStorageContents(ItemStack[] items) throws IllegalArgumentException {
		if (items.length > 0)
			throw new IllegalArgumentException("This inventory does not support items");
	}

	@Override
	public boolean contains(int materialId) {
		return false;
	}

	@Override
	public boolean contains(Material material) throws IllegalArgumentException {
		return false;
	}

	@Override
	public boolean contains(ItemStack item) {
		return false;
	}

	@Override
	public boolean contains(int materialId, int amount) {
		return false;
	}

	@Override
	public boolean contains(Material material, int amount) throws IllegalArgumentException {
		return false;
	}

	@Override
	public boolean contains(ItemStack item, int amount) {
		return false;
	}

	@Override
	public boolean containsAtLeast(ItemStack item, int amount) {
		return false;
	}

	@Override
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
	}

	@Override
	public void clear() {
	}

	@Override
	public List<HumanEntity> getViewers() {
		return new ArrayList<>(0);
	}

	@Override
	public String getTitle() {
		return "Player inventory";
	}

	@Override
	public InventoryType getType() {
		return InventoryType.PLAYER;
	}

	private ListIterator<ItemStack> iterator = new ArrayList<ItemStack>(0).listIterator();

	@Override
	public ListIterator<ItemStack> iterator() {
		return iterator;
	}

	@Override
	public ListIterator<ItemStack> iterator(int index) {
		return iterator;
	}

	@Override
	public Location getLocation() {
		return holder.getLocation();
	}

	@Override
	public void setItem(int index, ItemStack item) {
	}

	private HumanEntity holder;

	@Override
	public HumanEntity getHolder() {
		return holder;
	}
}
