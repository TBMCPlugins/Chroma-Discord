package buttondevteam.discordplugin;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.objenesis.ObjenesisStd;

import com.mojang.authlib.GameProfile;

import buttondevteam.discordplugin.listeners.MCChatListener;
import buttondevteam.lib.TBMCCoreAPI;
import lombok.val;
import net.minecraft.server.v1_12_R1.AdvancementDataPlayer;
import net.minecraft.server.v1_12_R1.DedicatedPlayerList;
import net.minecraft.server.v1_12_R1.DedicatedServer;
import net.minecraft.server.v1_12_R1.Entity;
import net.minecraft.server.v1_12_R1.EntityHuman;
import net.minecraft.server.v1_12_R1.EntityPlayer;
import net.minecraft.server.v1_12_R1.GameProfileBanList;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.IpBanList;
import net.minecraft.server.v1_12_R1.LoginListener;
import net.minecraft.server.v1_12_R1.MinecraftServer;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import net.minecraft.server.v1_12_R1.NetworkManager;
import net.minecraft.server.v1_12_R1.OpList;
import net.minecraft.server.v1_12_R1.Packet;
import net.minecraft.server.v1_12_R1.ScoreboardServer;
import net.minecraft.server.v1_12_R1.ServerStatisticManager;
import net.minecraft.server.v1_12_R1.WhiteList;
import net.minecraft.server.v1_12_R1.World;
import net.minecraft.server.v1_12_R1.WorldServer;

public class PlayerListWatcher extends DedicatedPlayerList {
	private DedicatedPlayerList plist;

	public PlayerListWatcher(DedicatedServer minecraftserver) {
		super(minecraftserver); // <-- Does some init stuff and calls Bukkit.setServer() so we have to use Objenesis
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent, boolean flag) {
		super.sendMessage(ichatbasecomponent, flag);
		MCChatListener.sendSystemMessageToChat(ichatbasecomponent.toPlainText());
	}

	public static void hookUp() {
		try {
			Field conf = CraftServer.class.getDeclaredField("console");
			conf.setAccessible(true);
			val server = (MinecraftServer) conf.get(Bukkit.getServer());
			val plw = new ObjenesisStd().newInstance(PlayerListWatcher.class); // Cannot call super constructor
			plw.plist = (DedicatedPlayerList) server.getPlayerList();
			plw.maxPlayers = plw.plist.getMaxPlayers();
			Field plf = plw.getClass().getField("players");
			plf.setAccessible(true);
			Field modf = plf.getClass().getDeclaredField("modifiers");
			modf.setAccessible(true);
			modf.set(plf, plf.getModifiers() & ~Modifier.FINAL);
			plf.set(plw, plw.plist.players);
			server.a(plw);
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Error while hacking the player list!", e);
		}
	}

	public void a(EntityHuman entityhuman, IChatBaseComponent ichatbasecomponent) {
		plist.a(entityhuman, ichatbasecomponent);
	}

	public ServerStatisticManager a(EntityHuman entityhuman) {
		return plist.a(entityhuman);
	}

	public void a(EntityPlayer entityplayer, int i) {
		plist.a(entityplayer, i);
	}

	public void a(EntityPlayer entityplayer, WorldServer worldserver) {
		plist.a(entityplayer, worldserver);
	}

	public NBTTagCompound a(EntityPlayer entityplayer) {
		return plist.a(entityplayer);
	}

	public void a(int i) {
		plist.a(i);
	}

	public void a(NetworkManager networkmanager, EntityPlayer entityplayer) {
		plist.a(networkmanager, entityplayer);
	}

	public void a(Packet<?> packet, int i) {
		plist.a(packet, i);
	}

	public EntityPlayer a(UUID uuid) {
		return plist.a(uuid);
	}

	public void addOp(GameProfile gameprofile) {
		plist.addOp(gameprofile);
	}

	public void addWhitelist(GameProfile gameprofile) {
		plist.addWhitelist(gameprofile);
	}

	public EntityPlayer attemptLogin(LoginListener loginlistener, GameProfile gameprofile, String hostname) {
		return plist.attemptLogin(loginlistener, gameprofile, hostname);
	}

	public String b(boolean flag) {
		return plist.b(flag);
	}

	public void b(EntityHuman entityhuman, IChatBaseComponent ichatbasecomponent) {
		plist.b(entityhuman, ichatbasecomponent);
	}

	public void b(EntityPlayer entityplayer, WorldServer worldserver) {
		plist.b(entityplayer, worldserver);
	}

	public List<EntityPlayer> b(String s) {
		return plist.b(s);
	}

	public Location calculateTarget(Location enter, World target) {
		return plist.calculateTarget(enter, target);
	}

	public void changeDimension(EntityPlayer entityplayer, int i, TeleportCause cause) {
		plist.changeDimension(entityplayer, i, cause);
	}

	public void changeWorld(Entity entity, int i, WorldServer worldserver, WorldServer worldserver1) {
		plist.changeWorld(entity, i, worldserver, worldserver1);
	}

	public int d() {
		return plist.d();
	}

	public void d(EntityPlayer entityplayer) {
		plist.d(entityplayer);
	}

	public String disconnect(EntityPlayer entityplayer) {
		return plist.disconnect(entityplayer);
	}

	public boolean equals(Object obj) {
		return plist.equals(obj);
	}

	public String[] f() {
		return plist.f();
	}

	public void f(EntityPlayer entityplayer) {
		plist.f(entityplayer);
	}

	public boolean f(GameProfile gameprofile) {
		return plist.f(gameprofile);
	}

	public GameProfile[] g() {
		return plist.g();
	}

	public boolean getHasWhitelist() {
		return plist.getHasWhitelist();
	}

	public IpBanList getIPBans() {
		return plist.getIPBans();
	}

	public int getMaxPlayers() {
		return plist.getMaxPlayers();
	}

	public OpList getOPs() {
		return plist.getOPs();
	}

	public EntityPlayer getPlayer(String s) {
		return plist.getPlayer(s);
	}

	public int getPlayerCount() {
		return plist.getPlayerCount();
	}

	public GameProfileBanList getProfileBans() {
		return plist.getProfileBans();
	}

	public String[] getSeenPlayers() {
		return plist.getSeenPlayers();
	}

	public DedicatedServer getServer() {
		return plist.getServer();
	}

	public WhiteList getWhitelist() {
		return plist.getWhitelist();
	}

	public String[] getWhitelisted() {
		return plist.getWhitelisted();
	}

	public AdvancementDataPlayer h(EntityPlayer entityplayer) {
		return plist.h(entityplayer);
	}

	public int hashCode() {
		return plist.hashCode();
	}

	public boolean isOp(GameProfile gameprofile) {
		return plist.isOp(gameprofile);
	}

	public boolean isWhitelisted(GameProfile gameprofile) {
		return plist.isWhitelisted(gameprofile);
	}

	public EntityPlayer moveToWorld(EntityPlayer entityplayer, int i, boolean flag, Location location,
			boolean avoidSuffocation) {
		return plist.moveToWorld(entityplayer, i, flag, location, avoidSuffocation);
	}

	public EntityPlayer moveToWorld(EntityPlayer entityplayer, int i, boolean flag) {
		return plist.moveToWorld(entityplayer, i, flag);
	}

	public String[] n() {
		return plist.n();
	}

	public void onPlayerJoin(EntityPlayer entityplayer, String joinMessage) {
		plist.onPlayerJoin(entityplayer, joinMessage);
	}

	public EntityPlayer processLogin(GameProfile gameprofile, EntityPlayer player) {
		return plist.processLogin(gameprofile, player);
	}

	public void reload() {
		plist.reload();
	}

	public void reloadWhitelist() {
		plist.reloadWhitelist();
	}

	public void removeOp(GameProfile gameprofile) {
		plist.removeOp(gameprofile);
	}

	public void removeWhitelist(GameProfile gameprofile) {
		plist.removeWhitelist(gameprofile);
	}

	public void repositionEntity(Entity entity, Location exit, boolean portal) {
		plist.repositionEntity(entity, exit, portal);
	}

	public int s() {
		return plist.s();
	}

	public void savePlayers() {
		plist.savePlayers();
	}

	@SuppressWarnings("rawtypes")
	public void sendAll(Packet packet, EntityHuman entityhuman) {
		plist.sendAll(packet, entityhuman);
	}

	@SuppressWarnings("rawtypes")
	public void sendAll(Packet packet, World world) {
		plist.sendAll(packet, world);
	}

	public void sendAll(Packet<?> packet) {
		plist.sendAll(packet);
	}

	public void sendMessage(IChatBaseComponent ichatbasecomponent) {
		plist.sendMessage(ichatbasecomponent);
	}

	public void sendMessage(IChatBaseComponent[] iChatBaseComponents) {
		plist.sendMessage(iChatBaseComponents);
	}

	public void sendPacketNearby(EntityHuman entityhuman, double d0, double d1, double d2, double d3, int i,
			Packet<?> packet) {
		plist.sendPacketNearby(entityhuman, d0, d1, d2, d3, i, packet);
	}

	public void sendScoreboard(ScoreboardServer scoreboardserver, EntityPlayer entityplayer) {
		plist.sendScoreboard(scoreboardserver, entityplayer);
	}

	public void setHasWhitelist(boolean flag) {
		plist.setHasWhitelist(flag);
	}

	public void setPlayerFileData(WorldServer[] aworldserver) {
		plist.setPlayerFileData(aworldserver);
	}

	public NBTTagCompound t() {
		return plist.t();
	}

	public void tick() {
		plist.tick();
	}

	public String toString() {
		return plist.toString();
	}

	public void u() {
		plist.u();
	}

	public void updateClient(EntityPlayer entityplayer) {
		plist.updateClient(entityplayer);
	}

	public List<EntityPlayer> v() {
		return plist.v();
	}
}
