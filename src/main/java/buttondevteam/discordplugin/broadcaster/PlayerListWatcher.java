package buttondevteam.discordplugin.broadcaster;

import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.lib.TBMCCoreAPI;
import lombok.val;
import org.bukkit.Bukkit;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

	static boolean hookUpDown(boolean up) throws Exception {
		val csc = Bukkit.getServer().getClass();
		Field conf = csc.getDeclaredField("console");
		conf.setAccessible(true);
		val server = conf.get(Bukkit.getServer());
		val nms = server.getClass().getPackage().getName();
		val dplc = Class.forName(nms + ".DedicatedPlayerList");
		val currentPL = server.getClass().getMethod("getPlayerList").invoke(server);
		if (up) {
			val icbcl = Class.forName(nms + ".IChatBaseComponent");
			val sendMessage = server.getClass().getMethod("sendMessage", icbcl);
			val cmtcl = Class.forName(nms + ".ChatMessageType");
			val systemType = cmtcl.getDeclaredField("SYSTEM").get(null);
			val chatType = cmtcl.getDeclaredField("CHAT").get(null);

			val obc = csc.getPackage().getName();
			val ccmcl = Class.forName(obc + ".util.CraftChatMessage");
			val fixComponent = ccmcl.getMethod("fixComponent", icbcl);
			val ppoc = Class.forName(nms + ".PacketPlayOutChat");
			val ppocC = Class.forName(nms + ".PacketPlayOutChat").getConstructor(icbcl, cmtcl);
			val sendAll = dplc.getMethod("sendAll", Class.forName(nms + ".Packet"));
			Method tpt;
			try {
				tpt = icbcl.getMethod("toPlainText");
			} catch (NoSuchMethodException e) {
				tpt = icbcl.getMethod("getString");
			}
			val toPlainText = tpt;
			mock = Mockito.mock(dplc, new Answer() { // Cannot call super constructor
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable {
					final Method method = invocation.getMethod();
					if (!method.getName().equals("sendMessage")) {
						if (method.getName().equals("sendAll")) {
							sendAll(invocation.getArgument(0));
							return null;
						}
						return method.invoke(plist, invocation.getArguments());
					}
					val args = invocation.getArguments();
					val params = method.getParameterTypes();
					if (params.length == 0) {
						TBMCCoreAPI.SendException("Found a strange method",
							new Exception("Found a sendMessage() method without arguments."));
						return null;
					}
					if (params[0].getSimpleName().equals("IChatBaseComponent[]"))
						for (val arg : (Object[]) args[0])
							sendMessage(arg, true);
					else if (params[0].getSimpleName().equals("IChatBaseComponent"))
						if (params.length > 1 && params[1].getSimpleName().equalsIgnoreCase("boolean"))
							sendMessage(args[0], (Boolean) args[1]);
						else
							sendMessage(args[0], true);
					else
						TBMCCoreAPI.SendException("Found a method with interesting params",
							new Exception("Found a sendMessage(" + params[0].getSimpleName() + ") method"));
					return null;
				}

				private void sendMessage(Object chatComponent, boolean system) {
					try { //Converted to use reflection
						sendMessage.invoke(server, chatComponent);
						Object chatmessagetype = system ? systemType : chatType;

						// CraftBukkit start - we run this through our processor first so we can get web links etc
						this.sendAll(ppocC.newInstance(fixComponent.invoke(null, chatComponent), chatmessagetype));
						// CraftBukkit end
					} catch (Exception e) {
						TBMCCoreAPI.SendException("An error occurred while passing a vanilla message through the player list", e);
					}
				}

				private void sendAll(Object packet) {
					try { // Some messages get sent by directly constructing a packet
						sendAll.invoke(plist, packet);
						if (packet.getClass() == ppoc) {
							Field msgf = ppoc.getDeclaredField("a");
							msgf.setAccessible(true);
							MCChatUtils.forAllMCChat(MCChatUtils.send((String) toPlainText.invoke(msgf.get(packet))));
						}
					} catch (Exception e) {
						TBMCCoreAPI.SendException("Failed to broadcast message sent to all players - hacking failed.", e);
					}
				}
			});
			plist = currentPL;
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
		} else {
			if (!(mock instanceof PlayerListWatcher))
				return false;
		}
		try {
			server.getClass().getMethod("a", dplc).invoke(server, up ? mock : plist);
		} catch (NoSuchMethodException e) {
			server.getClass().getMethod("a", Class.forName(server.getClass().getPackage().getName() + ".PlayerList"))
				.invoke(server, up ? mock : plist);
		}
		Field pllf = csc.getDeclaredField("playerList");
		pllf.setAccessible(true);
		pllf.set(Bukkit.getServer(), up ? mock : plist);
		return true;
	}
}
