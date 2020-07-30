package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import lombok.Getter;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.entity.Player;
import org.mockito.Answers;
import org.mockito.Mockito;

import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Same as {@link VanillaCommandListener14} but with reflection
 */
public class VanillaCommandListener15<T extends DiscordSenderBase & IMCPlayer<T>> {
	private @Getter T player;
	private static Class<?> vcwcl;
	private static String nms;

	protected VanillaCommandListener15(T player, Player bukkitplayer) {
		this.player = player;
		if (bukkitplayer != null && !bukkitplayer.getClass().getSimpleName().endsWith("CraftPlayer"))
			throw new ClassCastException("bukkitplayer must be a Bukkit player!");
	}

	/**
	 * This method will only send raw vanilla messages to the sender in plain text.
	 *
	 * @param player The Discord sender player (the wrapper)
	 */
	public static <T extends DiscordSenderBase & IMCPlayer<T>> VanillaCommandListener15<T> create(T player) throws Exception {
		return create(player, null);
	}

	/**
	 * This method will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
	 *
	 * @param player       The Discord sender player (the wrapper)
	 * @param bukkitplayer The Bukkit player to send the raw message to
	 */
	@SuppressWarnings("unchecked")
	public static <T extends DiscordSenderBase & IMCPlayer<T>> VanillaCommandListener15<T> create(T player, Player bukkitplayer) throws Exception {
		if (vcwcl == null) {
			String pkg = Bukkit.getServer().getClass().getPackage().getName();
			vcwcl = Class.forName(pkg + ".command.VanillaCommandWrapper");
		}
		if (nms == null) {
			var server = Bukkit.getServer();
			nms = server.getClass().getMethod("getServer").invoke(server).getClass().getPackage().getName(); //org.mockito.codegen
		}
		var iclcl = Class.forName(nms + ".ICommandListener");
		return Mockito.mock(VanillaCommandListener15.class, Mockito.withSettings().stubOnly()
			.useConstructor(player, bukkitplayer).extraInterfaces(iclcl).defaultAnswer(invocation -> {
				if (invocation.getMethod().getName().equals("sendMessage")) {
					var icbc = invocation.getArgument(0);
					player.sendMessage((String) icbc.getClass().getMethod("getString").invoke(icbc));
					if (bukkitplayer != null) {
						var handle = bukkitplayer.getClass().getMethod("getHandle").invoke(bukkitplayer);
						handle.getClass().getMethod("sendMessage", icbc.getClass()).invoke(handle, icbc);
					}
					return null;
				}
				if (!Modifier.isAbstract(invocation.getMethod().getModifiers()))
					return invocation.callRealMethod();
				if (invocation.getMethod().getReturnType() == boolean.class)
					return true; //shouldSend... shouldBroadcast...
				if (invocation.getMethod().getReturnType() == CommandSender.class)
					return player;
				return Answers.RETURNS_DEFAULTS.answer(invocation);
			}));
	}

	public static boolean runBukkitOrVanillaCommand(DiscordSenderBase dsender, String cmdstr) throws Exception {
		var server = Bukkit.getServer();
		var cmap = (SimpleCommandMap) server.getClass().getMethod("getCommandMap").invoke(server);
		val cmd = cmap.getCommand(cmdstr.split(" ")[0].toLowerCase());
		if (!(dsender instanceof Player) || !vcwcl.isAssignableFrom(cmd.getClass()))
			return Bukkit.dispatchCommand(dsender, cmdstr); // Unconnected users are treated well in vanilla cmds

		if (!(dsender instanceof IMCPlayer))
			throw new ClassCastException(
				"dsender needs to implement IMCPlayer to use vanilla commands as it implements Player.");

		IMCPlayer<?> sender = (IMCPlayer<?>) dsender; // Don't use val on recursive interfaces :P

		if (!(Boolean) vcwcl.getMethod("testPermission", CommandSender.class).invoke(cmd, sender))
			return true;

		var cworld = Bukkit.getWorlds().get(0);
		val world = cworld.getClass().getMethod("getHandle").invoke(cworld);
		var icommandlistener = sender.getVanillaCmdListener().getListener();
		if (icommandlistener == null)
			return VCMDWrapper.compatResponse(dsender);
		var clwcl = Class.forName(nms + ".CommandListenerWrapper");
		var v3dcl = Class.forName(nms + ".Vec3D");
		var v2fcl = Class.forName(nms + ".Vec2F");
		var icbcl = Class.forName(nms + ".IChatBaseComponent");
		var mcscl = Class.forName(nms + ".MinecraftServer");
		var ecl = Class.forName(nms + ".Entity");
		var cctcl = Class.forName(nms + ".ChatComponentText");
		var iclcl = Class.forName(nms + ".ICommandListener");
		Object wrapper = clwcl.getConstructor(iclcl, v3dcl, v2fcl, world.getClass(), int.class, String.class, icbcl, mcscl, ecl)
			.newInstance(icommandlistener,
				v3dcl.getConstructor(double.class, double.class, double.class).newInstance(0, 0, 0),
				v2fcl.getConstructor(float.class, float.class).newInstance(0, 0),
				world, 0, sender.getName(), cctcl.getConstructor(String.class).newInstance(sender.getName()),
				world.getClass().getMethod("getMinecraftServer").invoke(world), null);
		/*val wrapper = new CommandListenerWrapper(icommandlistener, new Vec3D(0, 0, 0),
			new Vec2F(0, 0), world, 0, sender.getName(),
			new ChatComponentText(sender.getName()), world.getMinecraftServer(), null);*/
		var pncscl = Class.forName(vcwcl.getPackage().getName() + ".ProxiedNativeCommandSender");
		Object pncs = pncscl.getConstructor(clwcl, CommandSender.class, CommandSender.class)
			.newInstance(wrapper, sender, sender);
		String[] args = cmdstr.split(" ");
		args = Arrays.copyOfRange(args, 1, args.length);
		try {
			return cmd.execute((CommandSender) pncs, cmd.getLabel(), args);
		} catch (Exception commandexception) {
			if (!commandexception.getClass().getSimpleName().equals("CommandException"))
				throw commandexception;
			// Taken from CommandHandler
			var cmcl = Class.forName(nms + ".ChatMessage");
			var chatmessage = cmcl.getConstructor(String.class, Object[].class)
				.newInstance(commandexception.getMessage(),
					new Object[]{commandexception.getClass().getMethod("a").invoke(commandexception)});
			var modifier = cmcl.getMethod("getChatModifier").invoke(chatmessage);
			var ecfcl = Class.forName(nms + ".EnumChatFormat");
			modifier.getClass().getMethod("setColor", ecfcl).invoke(modifier, ecfcl.getField("RED").get(null));
			icommandlistener.getClass().getMethod("sendMessage", icbcl).invoke(icommandlistener, chatmessage);
		}
		return true;
	}
}
