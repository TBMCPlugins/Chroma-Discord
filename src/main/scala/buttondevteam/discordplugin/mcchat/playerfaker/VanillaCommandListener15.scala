package buttondevteam.discordplugin.mcchat.playerfaker

import buttondevteam.discordplugin.mcchat.sender.{DiscordSenderBase, IMCPlayer}
import org.bukkit.Bukkit
import org.bukkit.command.{CommandSender, SimpleCommandMap}
import org.bukkit.entity.Player
import org.mockito.{Answers, Mockito}

import java.lang.reflect.Modifier
import java.util

/**
 * Same as {@link VanillaCommandListener14} but with reflection
 */
object VanillaCommandListener15 {
    private var vcwcl: Class[_] = null
    private var nms: String = null

    /**
     * This method will only send raw vanilla messages to the sender in plain text.
     *
     * @param player The Discord sender player (the wrapper)
     */
    @throws[Exception]
    def create[T <: DiscordSenderBase with IMCPlayer[T]](player: T): VanillaCommandListener15[T] = create(player, null)

    /**
     * This method will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
     *
     * @param player       The Discord sender player (the wrapper)
     * @param bukkitplayer The Bukkit player to send the raw message to
     */
    @SuppressWarnings(Array("unchecked"))
    @throws[Exception]
    def create[T <: DiscordSenderBase with IMCPlayer[T]](player: T, bukkitplayer: Player): VanillaCommandListener15[T] = {
        if (vcwcl == null) {
            val pkg = Bukkit.getServer.getClass.getPackage.getName
            vcwcl = Class.forName(pkg + ".command.VanillaCommandWrapper")
        }
        if (nms == null) {
            val server = Bukkit.getServer
            nms = server.getClass.getMethod("getServer").invoke(server).getClass.getPackage.getName //org.mockito.codegen
        }
        val iclcl = Class.forName(nms + ".ICommandListener")
        Mockito.mock(classOf[VanillaCommandListener15[T]],
            Mockito.withSettings.stubOnly.useConstructor(player, bukkitplayer)
                .extraInterfaces(iclcl).defaultAnswer(invocation => {
                if (invocation.getMethod.getName == "sendMessage") {
                    val icbc = invocation.getArgument(0)
                    player.sendMessage(icbc.getClass.getMethod("getString").invoke(icbc).asInstanceOf[String])
                    if (bukkitplayer != null) {
                        val handle = bukkitplayer.getClass.getMethod("getHandle").invoke(bukkitplayer)
                        handle.getClass.getMethod("sendMessage", icbc.getClass).invoke(handle, icbc)
                    }
                    null
                }
                else if (!Modifier.isAbstract(invocation.getMethod.getModifiers)) invocation.callRealMethod
                else if (invocation.getMethod.getReturnType eq classOf[Boolean]) true //shouldSend... shouldBroadcast...
                else if (invocation.getMethod.getReturnType eq classOf[CommandSender]) player
                else Answers.RETURNS_DEFAULTS.answer(invocation)
            }))
    }

    @throws[Exception]
    def runBukkitOrVanillaCommand(dsender: DiscordSenderBase, cmdstr: String): Boolean = {
        val server = Bukkit.getServer
        val cmap = server.getClass.getMethod("getCommandMap").invoke(server).asInstanceOf[SimpleCommandMap]
        val cmd = cmap.getCommand(cmdstr.split(" ")(0).toLowerCase)
        if (!dsender.isInstanceOf[Player] || cmd == null || !vcwcl.isAssignableFrom(cmd.getClass))
            return Bukkit.dispatchCommand(dsender, cmdstr) // Unconnected users are treated well in vanilla cmds
        if (!dsender.isInstanceOf[IMCPlayer[_]])
            throw new ClassCastException("dsender needs to implement IMCPlayer to use vanilla commands as it implements Player.")
        val sender = dsender.asInstanceOf[IMCPlayer[_]] // Don't use val on recursive interfaces :P
        if (!vcwcl.getMethod("testPermission", classOf[CommandSender]).invoke(cmd, sender).asInstanceOf[Boolean])
            return true
        val cworld = Bukkit.getWorlds.get(0)
        val world = cworld.getClass.getMethod("getHandle").invoke(cworld)
        val icommandlistener = sender.getVanillaCmdListener.getListener
        if (icommandlistener == null) return VCMDWrapper.compatResponse(dsender)
        val clwcl = Class.forName(nms + ".CommandListenerWrapper")
        val v3dcl = Class.forName(nms + ".Vec3D")
        val v2fcl = Class.forName(nms + ".Vec2F")
        val icbcl = Class.forName(nms + ".IChatBaseComponent")
        val mcscl = Class.forName(nms + ".MinecraftServer")
        val ecl = Class.forName(nms + ".Entity")
        val cctcl = Class.forName(nms + ".ChatComponentText")
        val iclcl = Class.forName(nms + ".ICommandListener")
        val wrapper = clwcl.getConstructor(iclcl, v3dcl, v2fcl, world.getClass, classOf[Int], classOf[String], icbcl, mcscl, ecl)
            .newInstance(icommandlistener, v3dcl.getConstructor(classOf[Double], classOf[Double], classOf[Double])
                .newInstance(0, 0, 0), v2fcl.getConstructor(classOf[Float], classOf[Float])
                .newInstance(0, 0), world, 0, sender.getName, cctcl.getConstructor(classOf[String])
                .newInstance(sender.getName), world.getClass.getMethod("getMinecraftServer").invoke(world), null)
        /*val wrapper = new CommandListenerWrapper(icommandlistener, new Vec3D(0, 0, 0),
              new Vec2F(0, 0), world, 0, sender.getName(),
              new ChatComponentText(sender.getName()), world.getMinecraftServer(), null);*/
        val pncscl = Class.forName(vcwcl.getPackage.getName + ".ProxiedNativeCommandSender")
        val pncs = pncscl.getConstructor(clwcl, classOf[CommandSender], classOf[CommandSender])
            .newInstance(wrapper, sender, sender)
        var args = cmdstr.split(" ")
        args = util.Arrays.copyOfRange(args, 1, args.length)
        try return cmd.execute(pncs.asInstanceOf[CommandSender], cmd.getLabel, args)
        catch {
            case commandexception: Exception =>
                if (!(commandexception.getClass.getSimpleName == "CommandException")) throw commandexception
                // Taken from CommandHandler
                val cmcl = Class.forName(nms + ".ChatMessage")
                val chatmessage = cmcl.getConstructor(classOf[String], classOf[Array[AnyRef]])
                    .newInstance(commandexception.getMessage, Array[AnyRef](commandexception.getClass.getMethod("a").invoke(commandexception)))
                val modifier = cmcl.getMethod("getChatModifier").invoke(chatmessage)
                val ecfcl = Class.forName(nms + ".EnumChatFormat")
                modifier.getClass.getMethod("setColor", ecfcl).invoke(modifier, ecfcl.getField("RED").get(null))
                icommandlistener.getClass.getMethod("sendMessage", icbcl).invoke(icommandlistener, chatmessage)
        }
        true
    }
}

class VanillaCommandListener15[T <: DiscordSenderBase with IMCPlayer[T]] protected(var player: T, val bukkitplayer: Player) {
    if (bukkitplayer != null && !bukkitplayer.getClass.getSimpleName.endsWith("CraftPlayer"))
        throw new ClassCastException("bukkitplayer must be a Bukkit player!")

    def getPlayer: T = this.player
}