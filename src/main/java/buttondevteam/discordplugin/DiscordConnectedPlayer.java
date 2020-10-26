package buttondevteam.discordplugin;

import buttondevteam.discordplugin.mcchat.MinecraftChatModule;
import buttondevteam.discordplugin.playerfaker.DiscordInventory;
import buttondevteam.discordplugin.playerfaker.VCMDWrapper;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Delegate;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.ServerOperator;
import org.mockito.MockSettings;
import org.mockito.Mockito;

import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.util.*;

import static org.mockito.Answers.RETURNS_DEFAULTS;

public abstract class DiscordConnectedPlayer extends DiscordSenderBase implements IMCPlayer<DiscordConnectedPlayer> {
	private @Getter VCMDWrapper vanillaCmdListener;
	@Getter
	@Setter
	private boolean loggedIn = false;

	@Delegate(excludes = ServerOperator.class)
	private PermissibleBase origPerm;

	private @Getter String name;

	private @Getter OfflinePlayer basePlayer;

	@Getter
	@Setter
	private PermissibleBase perm;

	private Location location;

	private final MinecraftChatModule module;

	@Getter
	private final UUID uniqueId;

	/**
	 * The parameters must match with {@link #create(User, MessageChannel, UUID, String, MinecraftChatModule)}
	 */
	protected DiscordConnectedPlayer(User user, MessageChannel channel, UUID uuid, String mcname,
	                                 MinecraftChatModule module) {
		super(user, channel);
		location = Bukkit.getWorlds().get(0).getSpawnLocation();
		origPerm = perm = new PermissibleBase(basePlayer = Bukkit.getOfflinePlayer(uuid));
		name = mcname;
		this.module = module;
		uniqueId = uuid;
		displayName = mcname;
		vanillaCmdListener = new VCMDWrapper(VCMDWrapper.createListener(this, module));
	}

	/**
	 * For testing
	 */
	protected DiscordConnectedPlayer(User user, MessageChannel channel) {
		super(user, channel);
		module = null;
		uniqueId = UUID.randomUUID();
	}

	public void setOp(boolean value) { //CraftPlayer-compatible implementation
		this.origPerm.setOp(value);
		this.perm.recalculatePermissions();
	}

	public boolean isOp() { return this.origPerm.isOp(); }

	@Override
	public boolean teleport(Location location) {
		if (module.allowFakePlayerTeleports.get())
			this.location = location;
		return true;
	}

	@Override
	public boolean teleport(Location location, PlayerTeleportEvent.TeleportCause cause) {
		if (module.allowFakePlayerTeleports.get())
			this.location = location;
		return true;
	}

	@Override
	public boolean teleport(Entity destination) {
		if (module.allowFakePlayerTeleports.get())
			this.location = destination.getLocation();
		return true;
	}

	@Override
	public boolean teleport(Entity destination, PlayerTeleportEvent.TeleportCause cause) {
		if (module.allowFakePlayerTeleports.get())
			this.location = destination.getLocation();
		return true;
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
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public void sendRawMessage(String message) {
		sendMessage(message);
	}

	@Override
	public void chat(String msg) {
		Bukkit.getPluginManager()
			.callEvent(new AsyncPlayerChatEvent(true, this, msg, new HashSet<>(Bukkit.getOnlinePlayers())));
	}

	@Override
	public World getWorld() {
		return Bukkit.getWorlds().get(0);
	}

	@Override
	public boolean isOnline() {
		return true;
	}

	@Override
	public Location getLocation() {
		return new Location(getWorld(), location.getX(), location.getY(), location.getZ(),
			location.getYaw(), location.getPitch());
	}

	@Override
	public Location getEyeLocation() {
		return getLocation();
	}

	@Override
	@Deprecated
	public double getMaxHealth() {
		return 20;
	}

	@Override
	public Player getPlayer() {
		return this;
	}

	@Getter
	@Setter
	private String displayName;

	@Override
	public AttributeInstance getAttribute(Attribute attribute) {
		return new AttributeInstance() {
			@Override
			public Attribute getAttribute() {
				return attribute;
			}

			@Override
			public double getBaseValue() {
				return getDefaultValue();
			}

			@Override
			public void setBaseValue(double value) {
			}

			@Override
			public Collection<AttributeModifier> getModifiers() {
				return Collections.emptyList();
			}

			@Override
			public void addModifier(AttributeModifier modifier) {
			}

			@Override
			public void removeModifier(AttributeModifier modifier) {
			}

			@Override
			public double getValue() {
				return getDefaultValue();
			}

			@Override
			public double getDefaultValue() {
				return 20; //Works for max health, should be okay for the rest
			}
		};
	}

	@Override
	public GameMode getGameMode() {
		return GameMode.SPECTATOR;
	}

	@SuppressWarnings("deprecation")
	private final Player.Spigot spigot = new Player.Spigot() {
		@Override
		public InetSocketAddress getRawAddress() {
			return null;
		}

		@Override
		public void playEffect(Location location, Effect effect, int id, int data, float offsetX, float offsetY, float offsetZ, float speed, int particleCount, int radius) {
		}

		@Override
		public boolean getCollidesWithEntities() {
			return false;
		}

		@Override
		public void setCollidesWithEntities(boolean collides) {
		}

		@Override
		public void respawn() {
		}

		@Override
		public String getLocale() {
			return "en_us";
		}

		@Override
		public Set<Player> getHiddenPlayers() {
			return Collections.emptySet();
		}

		@Override
		public void sendMessage(BaseComponent component) {
			DiscordConnectedPlayer.super.sendMessage(component.toPlainText());
		}

		@Override
		public void sendMessage(BaseComponent... components) {
			for (var component : components)
				sendMessage(component);
		}

		@Override
		public void sendMessage(ChatMessageType position, BaseComponent component) {
			sendMessage(component); //Ignore position
		}

		@Override
		public void sendMessage(ChatMessageType position, BaseComponent... components) {
			sendMessage(components); //Ignore position
		}

		@Override
		public boolean isInvulnerable() {
			return true;
		}
	};

	@Override
	public Player.Spigot spigot() {
		return spigot;
	}

	public static DiscordConnectedPlayer create(User user, MessageChannel channel, UUID uuid, String mcname,
	                                            MinecraftChatModule module) {
		return Mockito.mock(DiscordConnectedPlayer.class,
			getSettings().useConstructor(user, channel, uuid, mcname, module));
	}

	public static DiscordConnectedPlayer createTest() {
		return Mockito.mock(DiscordConnectedPlayer.class, getSettings().useConstructor(null, null));
	}

	private static MockSettings getSettings() {
		return Mockito.withSettings()
			.defaultAnswer(invocation -> {
				try {
					if (!Modifier.isAbstract(invocation.getMethod().getModifiers()))
						return invocation.callRealMethod();
					if (PlayerInventory.class.isAssignableFrom(invocation.getMethod().getReturnType()))
						return Mockito.mock(DiscordInventory.class, Mockito.withSettings().extraInterfaces(PlayerInventory.class));
					if (Inventory.class.isAssignableFrom(invocation.getMethod().getReturnType()))
						return new DiscordInventory();
					return RETURNS_DEFAULTS.answer(invocation);
				} catch (Exception e) {
					System.err.println("Error in mocked player!");
					e.printStackTrace();
					return RETURNS_DEFAULTS.answer(invocation);
				}
			})
			.stubOnly();
	}
}
