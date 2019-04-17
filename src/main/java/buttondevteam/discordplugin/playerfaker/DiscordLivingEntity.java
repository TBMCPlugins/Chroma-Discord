package buttondevteam.discordplugin.playerfaker;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.MessageChannel;

import java.util.*;

public abstract class DiscordLivingEntity extends DiscordEntity implements LivingEntity {

	protected DiscordLivingEntity(IUser user, MessageChannel channel, int entityId, UUID uuid) {
		super(user, channel, entityId, uuid);
	}

	private @Getter EntityEquipment equipment = new DiscordEntityEquipment(this);

	@Getter
	@Setter
	private static class DiscordEntityEquipment implements EntityEquipment {

		private float leggingsDropChance;
		private ItemStack leggings;
		private float itemInOffHandDropChance;
		private ItemStack itemInOffHand;
		private float itemInMainHandDropChance;
		private ItemStack itemInMainHand;
		private float itemInHandDropChance;
		private ItemStack itemInHand;
		private float helmetDropChance;
		private ItemStack helmet;
		private float chestplateDropChance;
		private ItemStack chestplate;
		private float bootsDropChance;
		private ItemStack boots;
		private ItemStack[] armorContents = new ItemStack[0]; // TODO
		private final Entity holder;

		public DiscordEntityEquipment(Entity holder) {
			this.holder = holder;
		}

		@Override
		public void clear() {
			armorContents = new ItemStack[0];
		}
	}

	@Override
	public AttributeInstance getAttribute(Attribute attribute) { // We don't support any attributes
		return null;
	}

	@Override
	public void damage(double amount) {
	}

	@Override
	public void damage(double amount, Entity source) {
	}

	@Override
	public double getHealth() {
		return getMaxHealth();
	}

	@Override
	public void setHealth(double health) {
	}

	@Override
	public double getMaxHealth() {
		return 100;
	}

	@Override
	public void setMaxHealth(double health) {
	}

	@Override
	public void resetMaxHealth() {
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> projectile) {
		return null;
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> projectile, Vector velocity) {
		return null;
	}

	@Override
	public double getEyeHeight() {
		return 0;
	}

	@Override
	public double getEyeHeight(boolean ignoreSneaking) {
		return 0;
	}

	@Override
	public Location getEyeLocation() {
		return getLocation();
	}

	@Override
	public List<Block> getLineOfSight(Set<Material> transparent, int maxDistance) {
		return Arrays.asList();
	}

	@Override
	public Block getTargetBlock(HashSet<Byte> transparent, int maxDistance) {
		return null;
	}

	@Override
	public Block getTargetBlock(Set<Material> transparent, int maxDistance) {
		return null;
	}

	@Override
	public List<Block> getLastTwoTargetBlocks(HashSet<Byte> transparent, int maxDistance) {
		return Arrays.asList();
	}

	@Override
	public List<Block> getLastTwoTargetBlocks(Set<Material> transparent, int maxDistance) {
		return Arrays.asList();
	}

	@Override
	public int getRemainingAir() {
		return 100;
	}

	@Override
	public void setRemainingAir(int ticks) {
	}

	@Override
	public int getMaximumAir() {
		return 100;
	}

	@Override
	public void setMaximumAir(int ticks) {
	}

	@Override
	public int getMaximumNoDamageTicks() {
		return 100;
	}

	@Override
	public void setMaximumNoDamageTicks(int ticks) {
	}

	@Override
	public double getLastDamage() {
		return 0;
	}

	@Override
	public void setLastDamage(double damage) {
	}

	@Override
	public int getNoDamageTicks() {
		return 100;
	}

	@Override
	public void setNoDamageTicks(int ticks) {
	}

	@Override
	public Player getKiller() {
		return null;
	}

	@Override
	public boolean addPotionEffect(PotionEffect effect) {
		return false;
	}

	@Override
	public boolean addPotionEffect(PotionEffect effect, boolean force) {
		return false;
	}

	@Override
	public boolean addPotionEffects(Collection<PotionEffect> effects) {
		return false;
	}

	@Override
	public boolean hasPotionEffect(PotionEffectType type) {
		return false;
	}

	@Override
	public PotionEffect getPotionEffect(PotionEffectType type) {
		return null;
	}

	@Override
	public void removePotionEffect(PotionEffectType type) {
	}

	@Override
	public Collection<PotionEffect> getActivePotionEffects() {
		return Arrays.asList();
	}

	@Override
	public boolean hasLineOfSight(Entity other) {
		return false;
	}

	@Override
	public boolean getRemoveWhenFarAway() {
		return false;
	}

	@Override
	public void setRemoveWhenFarAway(boolean remove) {
	}

	@Override
	public void setCanPickupItems(boolean pickup) {
	}

	@Override
	public boolean getCanPickupItems() {
		return false;
	}

	@Override
	public boolean isLeashed() {
		return false;
	}

	@Override
	public Entity getLeashHolder() throws IllegalStateException {
		throw new IllegalStateException();
	}

	@Override
	public boolean setLeashHolder(Entity holder) {
		return false;
	}

	@Override
	public boolean isGliding() {
		return false;
	}

	@Override
	public void setGliding(boolean gliding) {
	}

	@Override
	public void setAI(boolean ai) {
	}

	@Override
	public boolean hasAI() {
		return false;
	}

	@Override
	public void setCollidable(boolean collidable) {
	}

	@Override
	public boolean isCollidable() {
		return false;
	}

}
