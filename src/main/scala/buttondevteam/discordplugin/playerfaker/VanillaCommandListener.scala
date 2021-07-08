package buttondevteam.discordplugin.playerfaker

import buttondevteam.discordplugin.{DiscordSenderBase, IMCPlayer}
import net.minecraft.server.v1_12_R1.*
import org.bukkit.Bukkit
import org.bukkit.craftbukkit.v1_12_R1.command.VanillaCommandWrapper
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer
import org.bukkit.craftbukkit.v1_12_R1.{CraftServer, CraftWorld}
import org.bukkit.entity.Player

import java.util

object VanillaCommandListener {
    def runBukkitOrVanillaCommand(dsender: DiscordSenderBase, cmdstr: String): Boolean = {
        val cmd = Bukkit.getServer.asInstanceOf[CraftServer].getCommandMap.getCommand(cmdstr.split(" ")(0).toLowerCase)
        if (!dsender.isInstanceOf[Player] || !cmd.isInstanceOf[VanillaCommandWrapper])
            return Bukkit.dispatchCommand(dsender, cmdstr) // Unconnected users are treated well in vanilla cmds
        if (!dsender.isInstanceOf[IMCPlayer[_]])
            throw new ClassCastException("dsender needs to implement IMCPlayer to use vanilla commands as it implements Player.")
        val sender = dsender.asInstanceOf[IMCPlayer[_]]
        val vcmd = cmd.asInstanceOf[VanillaCommandWrapper]
        if (!vcmd.testPermission(sender)) return true
        val icommandlistener = sender.getVanillaCmdListener.getListener.asInstanceOf[ICommandListener]
        if (icommandlistener == null) return VCMDWrapper.compatResponse(dsender)
        var args = cmdstr.split(" ")
        args = util.Arrays.copyOfRange(args, 1, args.length)
        try vcmd.dispatchVanillaCommand(sender, icommandlistener, args)
        catch {
            case commandexception: CommandException =>
                // Taken from CommandHandler
                val chatmessage = new ChatMessage(commandexception.getMessage, commandexception.getArgs)
                chatmessage.getChatModifier.setColor(EnumChatFormat.RED)
                icommandlistener.sendMessage(chatmessage)
        }
        true
    }
}

class VanillaCommandListener[T <: DiscordSenderBase with IMCPlayer[T]] extends ICommandListener {
    def getPlayer: T = this.player

    private var player: T = null.asInstanceOf
    private var bukkitplayer: Player = null

    /**
     * This constructor will only send raw vanilla messages to the sender in plain text.
     *
     * @param player The Discord sender player (the wrapper)
     */
    def this(player: T) = {
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
    def this(player: T, bukkitplayer: Player) = {
        this()
        this.player = player
        this.bukkitplayer = bukkitplayer
        if (bukkitplayer != null && !bukkitplayer.isInstanceOf[CraftPlayer])
            throw new ClassCastException("bukkitplayer must be a Bukkit player!")
    }

    override def C_(): MinecraftServer = Bukkit.getServer.asInstanceOf[CraftServer].getServer

    override def a(oplevel: Int, cmd: String): Boolean = { //return oplevel <= 2; // Value from CommandBlockListenerAbstract, found what it is in EntityPlayer - Wait, that'd always allow OP commands
        oplevel == 0 || player.isOp
    }

    override def getName: String = player.getName

    override def getWorld: World = player.getWorld.asInstanceOf[CraftWorld].getHandle

    override def sendMessage(arg0: IChatBaseComponent): Unit = {
        player.sendMessage(arg0.toPlainText)
        if (bukkitplayer != null) bukkitplayer.asInstanceOf[CraftPlayer].getHandle.sendMessage(arg0)
    }
}