package buttondevteam.discordplugin.playerfaker

import buttondevteam.discordplugin.{DiscordSenderBase, IMCPlayer}
import net.minecraft.server.v1_14_R1._
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.craftbukkit.v1_14_R1.command.{ProxiedNativeCommandSender, VanillaCommandWrapper}
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_14_R1.{CraftServer, CraftWorld}
import org.bukkit.entity.Player

import java.util

object VanillaCommandListener14 {
    def runBukkitOrVanillaCommand(dsender: DiscordSenderBase, cmdstr: String): Boolean = {
        val cmd = Bukkit.getServer.asInstanceOf[CraftServer].getCommandMap.getCommand(cmdstr.split(" ")(0).toLowerCase)
        if (!dsender.isInstanceOf[Player] || !cmd.isInstanceOf[VanillaCommandWrapper])
            return Bukkit.dispatchCommand(dsender, cmdstr) // Unconnected users are treated well in vanilla cmds
        if (!dsender.isInstanceOf[IMCPlayer[_]])
            throw new ClassCastException("dsender needs to implement IMCPlayer to use vanilla commands as it implements Player.")
        val sender = dsender.asInstanceOf[IMCPlayer[_]] // Don't use val on recursive interfaces :P
        val vcmd = cmd.asInstanceOf[VanillaCommandWrapper]
        if (!vcmd.testPermission(sender)) return true
        val world = Bukkit.getWorlds.get(0).asInstanceOf[CraftWorld].getHandle
        val icommandlistener = sender.getVanillaCmdListener.getListener.asInstanceOf[ICommandListener]
        if (icommandlistener == null) return VCMDWrapper.compatResponse(dsender)
        val wrapper = new CommandListenerWrapper(icommandlistener, new Vec3D(0, 0, 0), new Vec2F(0, 0), world, 0, sender.getName, new ChatComponentText(sender.getName), world.getMinecraftServer, null)
        val pncs = new ProxiedNativeCommandSender(wrapper, sender, sender)
        var args = cmdstr.split(" ")
        args = util.Arrays.copyOfRange(args, 1, args.length)
        try return vcmd.execute(pncs, cmd.getLabel, args)
        catch {
            case commandexception: CommandException =>
                // Taken from CommandHandler
                val chatmessage = new ChatMessage(commandexception.getMessage, commandexception.a)
                chatmessage.getChatModifier.setColor(EnumChatFormat.RED)
                icommandlistener.sendMessage(chatmessage)
        }
        true
    }
}

class VanillaCommandListener14[T <: DiscordSenderBase with IMCPlayer[T]] extends ICommandListener {
    def getPlayer: T = this.player

    private var player: T = null.asInstanceOf
    private var bukkitplayer: Player = null

    /**
     * This constructor will only send raw vanilla messages to the sender in plain text.
     *
     * @param player The Discord sender player (the wrapper)
     */
    def this(player: T) {
        this()
        this.player = player
        this.bukkitplayer = null
    }

    /**
     * This constructor will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
     *
     * @param player       The Discord sender player (the wrapper)
     * @param bukkitplayer The Bukkit player to send the raw message to
     */
    def this(player: T, bukkitplayer: Player) {
        this()
        this.player = player
        this.bukkitplayer = bukkitplayer
        if (bukkitplayer != null && !bukkitplayer.isInstanceOf[CraftPlayer]) throw new ClassCastException("bukkitplayer must be a Bukkit player!")
    }

    override def sendMessage(arg0: IChatBaseComponent): Unit = {
        player.sendMessage(arg0.getString)
        if (bukkitplayer != null) bukkitplayer.asInstanceOf[CraftPlayer].getHandle.sendMessage(arg0)
    }

    override def shouldSendSuccess = true

    override def shouldSendFailure = true

    override def shouldBroadcastCommands = true //Broadcast to in-game admins
    override def getBukkitSender(commandListenerWrapper: CommandListenerWrapper): CommandSender = player
}