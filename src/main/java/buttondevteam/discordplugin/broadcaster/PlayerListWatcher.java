package buttondevteam.discordplugin.broadcaster;

import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class PlayerListWatcher {
	private static Object plist;
	private static Object mock;

	/*public PlayerListWatcher(DedicatedServer minecraftserver) {
		super(minecraftserver); // <-- Does some init stuff and calls Bukkit.setServer() so we have to use Objenesis
	}

	public void sendAll(Packet<?> packet) {
		plist.sendAll(packet);
		try { // Some messages get sent by directly constructing a packet
			if (packet instanceof PacketPlayOutChat) {
				Field msgf = PacketPlayOutChat.class.getDeclaredField("a");
				msgf.setAccessible(true);
				MCChatUtils.forAllMCChat(MCChatUtils.send(((IChatBaseComponent) msgf.get(packet)).toPlainText()));
			}
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to broadcast message sent to all players - hacking failed.", e);
		}
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent, boolean flag) { // Needed so it calls the overridden method
		plist.getServer().sendMessage(ichatbasecomponent);
		ChatMessageType chatmessagetype = flag ? ChatMessageType.SYSTEM : ChatMessageType.CHAT;

		// CraftBukkit start - we run this through our processor first so we can get web links etc
		this.sendAll(new PacketPlayOutChat(CraftChatMessage.fixComponent(ichatbasecomponent), chatmessagetype));
		// CraftBukkit end
	}

	@Override
	public void sendMessage(IChatBaseComponent ichatbasecomponent) { // Needed so it calls the overriden method
		this.sendMessage(ichatbasecomponent, true);
	}

	@Override
	public void sendMessage(IChatBaseComponent[] iChatBaseComponents) { // Needed so it calls the overridden method
		for (IChatBaseComponent component : iChatBaseComponents) {
			sendMessage(component, true);
		}
	}*/

	static void hookUp() throws Exception {
		val csc = Bukkit.getServer().getClass();
		Field conf = csc.getDeclaredField("console");
		conf.setAccessible(true);
		val server = conf.get(Bukkit.getServer());
		val nms = server.getClass().getPackage().getName();
		val dplc = Class.forName(nms + ".DedicatedPlayerList");
		mock = Mockito.mock(dplc, new Answer() { // Cannot call super constructor
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				return invocation.getMethod().invoke(plist, invocation.getArguments());
			}
		});
		plist = server.getClass().getMethod("getPlayerList").invoke(server);
		try {
			Field mpf = mock.getClass().getField("maxPlayers");
			mpf.setAccessible(true);
			Field modf = mpf.getClass().getDeclaredField("modifiers");
			modf.setAccessible(true);
			modf.set(mpf, mpf.getModifiers() & ~Modifier.FINAL);
			mpf.set(mock, mpf.get(plist));
		} catch (NoSuchFieldException ignored) {
			//The field no longer exists on 1.14
		}
		Field plf = mock.getClass().getField("players");
		plf.setAccessible(true);
		Field modf = plf.getClass().getDeclaredField("modifiers");
		modf.setAccessible(true);
		modf.set(plf, plf.getModifiers() & ~Modifier.FINAL);
		plf.set(mock, plf.get(plist));
		try {
			server.getClass().getMethod("a", dplc).invoke(server, mock);
		} catch (NoSuchMethodException e) {
			server.getClass().getMethod("a", Class.forName(server.getClass().getPackage().getName() + ".PlayerList")).invoke(server, mock);
		}
		Field pllf = CraftServer.class.getDeclaredField("playerList");
		pllf.setAccessible(true);
		pllf.set(Bukkit.getServer(), mock);
	}

	static boolean hookDown() throws Exception {
		/*Field conf = CraftServer.class.getDeclaredField("console");
		conf.setAccessible(true);
		val server = (MinecraftServer) conf.get(Bukkit.getServer());
		val plist = (DedicatedPlayerList) server.getPlayerList();
		if (!(plist instanceof PlayerListWatcher))
			return false;
		server.a(((PlayerListWatcher) plist).plist);
		Field pllf = CraftServer.class.getDeclaredField("playerList");
		pllf.setAccessible(true);
		pllf.set(Bukkit.getServer(), ((PlayerListWatcher) plist).plist);*/
		return true;
	}
}
