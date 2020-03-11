package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.DiscordSenderBase;
import buttondevteam.discordplugin.IMCPlayer;
import lombok.Getter;
import lombok.val;
import net.minecraft.server.v1_12_R1.*;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.command.VanillaCommandWrapper;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.Arrays;

public class VanillaCommandListener<T extends DiscordSenderBase & IMCPlayer<T>> implements ICommandListener {
	private @Getter T player;
	private Player bukkitplayer;

	/**
	 * This constructor will only send raw vanilla messages to the sender in plain text.
	 *
	 * @param player The Discord sender player (the wrapper)
	 */
	public VanillaCommandListener(T player) {
		this.player = player;
		this.bukkitplayer = null;
	}

	/**
	 * This constructor will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
	 *
	 * @param player       The Discord sender player (the wrapper)
	 * @param bukkitplayer The Bukkit player to send the raw message to
	 */
	public VanillaCommandListener(T player, Player bukkitplayer) {
		this.player = player;
		this.bukkitplayer = bukkitplayer;
		if (!(bukkitplayer instanceof CraftPlayer))
			throw new ClassCastException("bukkitplayer must be a Bukkit player!");
	}

	@Override
	public MinecraftServer C_() {
		return ((CraftServer) Bukkit.getServer()).getServer();
	}

	@Override
	public boolean a(int oplevel, String cmd) {
		// return oplevel <= 2; // Value from CommandBlockListenerAbstract, found what it is in EntityPlayer - Wait, that'd always allow OP commands
		return oplevel == 0 || player.isOp();
	}

	@Override
	public String getName() {
		return player.getName();
	}

	@Override
	public World getWorld() {
		return ((CraftWorld) player.getWorld()).getHandle();
	}

	@Override
	public void sendMessage(IChatBaseComponent arg0) {
		player.sendMessage(arg0.toPlainText());
		if (bukkitplayer != null)
			((CraftPlayer) bukkitplayer).getHandle().sendMessage(arg0);
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

		ICommandListener icommandlistener = (ICommandListener) sender.getVanillaCmdListener().getListener();
		String[] args = cmdstr.split(" ");
		args = Arrays.copyOfRange(args, 1, args.length);
		try {
			vcmd.dispatchVanillaCommand(sender, icommandlistener, args);
		} catch (CommandException commandexception) {
			// Taken from CommandHandler
			ChatMessage chatmessage = new ChatMessage(commandexception.getMessage(), commandexception.getArgs());
			chatmessage.getChatModifier().setColor(EnumChatFormat.RED);
			icommandlistener.sendMessage(chatmessage);
		}
		return true;
	}
}
