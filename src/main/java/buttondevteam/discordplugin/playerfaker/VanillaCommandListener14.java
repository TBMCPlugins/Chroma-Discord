package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import lombok.Getter;
import lombok.val;
import net.minecraft.server.v1_14_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_14_R1.CraftServer;
import org.bukkit.craftbukkit.v1_14_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_14_R1.command.ProxiedNativeCommandSender;
import org.bukkit.craftbukkit.v1_14_R1.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class VanillaCommandListener14<T extends DiscordSenderBase & IMCPlayer<T>> implements ICommandListener {
	private @Getter T player;
	private Player bukkitplayer;

	/**
	 * This constructor will only send raw vanilla messages to the sender in plain text.
	 *
	 * @param player The Discord sender player (the wrapper)
	 */
	public VanillaCommandListener14(T player) {
		this.player = player;
		this.bukkitplayer = null;
	}

	/**
	 * This constructor will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
	 *
	 * @param player       The Discord sender player (the wrapper)
	 * @param bukkitplayer The Bukkit player to send the raw message to
	 */
	public VanillaCommandListener14(T player, Player bukkitplayer) {
		this.player = player;
		this.bukkitplayer = bukkitplayer;
		if (bukkitplayer != null && !(bukkitplayer instanceof CraftPlayer))
			throw new ClassCastException("bukkitplayer must be a Bukkit player!");
	}

	@Override
	public void sendMessage(IChatBaseComponent arg0) {
		player.sendMessage(arg0.getString());
		if (bukkitplayer != null)
			((CraftPlayer) bukkitplayer).getHandle().sendMessage(arg0);
	}

	@Override
	public boolean shouldSendSuccess() {
		return true;
	}

	@Override
	public boolean shouldSendFailure() {
		return true;
	}

	@Override
	public boolean shouldBroadcastCommands() {
		return true; //Broadcast to in-game admins
	}

	@Override
	public CommandSender getBukkitSender(CommandListenerWrapper commandListenerWrapper) {
		return player;
	}

	public static boolean runBukkitOrVanillaCommand(DiscordSenderBase dsender, String cmdstr) {
		val cmd = ((CraftServer) Bukkit.getServer()).getCommandMap().getCommand(cmdstr.split(" ")[0].toLowerCase());
		if (!(dsender instanceof Player) || !(cmd instanceof VanillaCommandWrapper))
			return Bukkit.dispatchCommand(dsender, cmdstr); // Unconnected users are treated well in vanilla cmds

		if (!(dsender instanceof IMCPlayer))
			throw new ClassCastException(
				"dsender needs to implement IMCPlayer to use vanilla commands as it implements Player.");

		IMCPlayer<?> sender = (IMCPlayer<?>) dsender; // Don't use val on recursive interfaces :P

		val vcmd = (VanillaCommandWrapper) cmd;
		if (!vcmd.testPermission(sender))
			return true;

		val world = ((CraftWorld) Bukkit.getWorlds().get(0)).getHandle();
		ICommandListener icommandlistener = (ICommandListener) sender.getVanillaCmdListener().getListener();
		val wrapper = new CommandListenerWrapper(icommandlistener, new Vec3D(0, 0, 0),
			new Vec2F(0, 0), world, 0, sender.getName(),
			new ChatComponentText(sender.getName()), world.getMinecraftServer(), null);
		val pncs = new ProxiedNativeCommandSender(wrapper, sender, sender);
		String[] args = cmdstr.split(" ");
		args = Arrays.copyOfRange(args, 1, args.length);
		try {
			return vcmd.execute(pncs, cmd.getLabel(), args);
		} catch (CommandException commandexception) {
			// Taken from CommandHandler
			ChatMessage chatmessage = new ChatMessage(commandexception.getMessage(), commandexception.a());
			chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
			icommandlistener.sendMessage(chatmessage);
		}
		return true;
	}
}
