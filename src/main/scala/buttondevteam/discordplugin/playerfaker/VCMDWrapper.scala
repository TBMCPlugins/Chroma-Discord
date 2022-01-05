package buttondevteam.discordplugin.playerfaker

import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.{DiscordSenderBase, IMCPlayer}
import buttondevteam.lib.TBMCCoreAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object VCMDWrapper {
    /**
     * This constructor will only send raw vanilla messages to the sender in plain text.
     *
     * @param player The Discord sender player (the wrapper)
     */
    def createListener[T <: DiscordSenderBase with IMCPlayer[T]](player: T, module: MinecraftChatModule): AnyRef =
        createListener(player, null, module)

    /**
     * This constructor will send both raw vanilla messages to the sender in plain text and forward the raw message to the provided player.
     *
     * @param player       The Discord sender player (the wrapper)
     * @param bukkitplayer The Bukkit player to send the raw message to
     * @param module       The Minecraft chat module
     */
    def createListener[T <: DiscordSenderBase with IMCPlayer[T]](player: T, bukkitplayer: Player, module: MinecraftChatModule): AnyRef = try {
        var ret: AnyRef = null
        val mcpackage = Bukkit.getServer.getClass.getPackage.getName
        if (mcpackage.contains("1_12")) ret = new VanillaCommandListener[T](player, bukkitplayer)
        else if (mcpackage.contains("1_14")) ret = new VanillaCommandListener14[T](player, bukkitplayer)
        else if (mcpackage.contains("1_15") || mcpackage.contains("1_16")) ret = VanillaCommandListener15.create(player, bukkitplayer) //bukkitplayer may be null but that's fine
        else ret = null
        if (ret == null) compatWarning(module)
        ret
    } catch {
        case e@(_: NoClassDefFoundError | _: Exception) =>
            compatWarning(module)
            TBMCCoreAPI.SendException("Failed to create vanilla command listener", e, module)
            null
    }

    private def compatWarning(module: MinecraftChatModule): Unit =
        module.logWarn("Vanilla commands won't be available from Discord due to a compatibility error. Disable vanilla command support to remove this message.")

    private[playerfaker] def compatResponse(dsender: DiscordSenderBase) = {
        dsender.sendMessage("Vanilla commands are not supported on this Minecraft version.")
        true
    }
}

class VCMDWrapper(private val listener: AnyRef) {
    @javax.annotation.Nullable def getListener: AnyRef = listener

    //Needed to mock the player @Nullable
}