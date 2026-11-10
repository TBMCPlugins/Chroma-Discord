package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.ChannelconBroadcast
import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.util.DPState
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.bukkit.Bukkit
import reactor.core.publisher.Mono
import java.util.*

/**
 * Provides Minecraft chat connection to Discord. Commands may be used either in a public chat (limited) or in a DM.
 */
class MinecraftChatModule : Component<DiscordPlugin>() {
    companion object {
        var state = DPState.RUNNING
    }

    var listener: MCChatListener? = null
        private set
    internal var disabling = false

    /**
     * A list of commands that can be used in public chats - Warning: Some plugins will treat players as OPs, always test before allowing a command!
     */
    val whitelistedCommands by config.getData(
        "whitelistedCommands",
        listOf("list", "u", "shrug", "tableflip", "unflip", "mwiki", "yeehaw", "lenny", "rp", "plugins")
    )

    /**
     * The channel to use as the public Minecraft chat - everything public gets broadcasted here
     */
    val chatChannel by DPUtils.snowflakeData(config, "chatChannel", 0L)

    val chatChannelMono = DPUtils.getMessageChannel(chatChannel.getPath, chatChannel.get)

    /**
     * The channel where the plugin can log when it mutes a player on Discord because of a Minecraft mute
     */
    val modlogChannel = DPUtils.channelData(config, "modlogChannel")

    /**
     * The plugins to exclude from fake player events used for the 'mcchat' command - some plugins may crash, add them here
     */
    val excludedPlugins by config.getData(
        "excludedPlugins",
        arrayOf("ProtocolLib", "LibsDisguises", "JourneyMapServer")
    )

    /**
     * If this is on, each chat channel will have a player list in their description.
     * It only gets added if there's no description yet or there are (at least) two lines of "----" following each other.
     * Note that it will replace <b>everything</b> above the first and below the last "----" but it will only detect exactly four dashes.
     * So if you want to use dashes for something else in the description, make sure it's either less or more dashes in one line.
     */
    val showPlayerListOnDC by config.getData("showPlayerListOnDC", true)

    /**
     * This setting controls whether custom chat connections can be <i>created</i> (existing connections will always work).
     * Custom chat connections can be created using the channelcon command and they allow players to display town chat in a Discord channel for example.
     * See the channelcon command for more details.
     */
    val allowCustomChat by config.getData("allowCustomChat", true)

    /**
     * This setting allows you to control if players can DM the bot to log on the server from Discord.
     * This allows them to both chat and perform any command they can in-game.
     */
    val allowPrivateChat by config.getData("allowPrivateChat", true)

    /**
     * If set, message authors appearing on Discord will link to this URL. A 'type' and 'id' parameter will be added with the user's platform (Discord, Minecraft, ...) and ID.
     */
    val profileURL by config.getData("profileURL", "")

    /**
     * Enables support for running vanilla commands through Discord, if you ever need it.
     */
    val enableVanillaCommands by config.getData("enableVanillaCommands", true)

    /**
     * Set by the component to report crashes.
     */
    private var serverUp by config.getData("serverUp", false)
    private val mcChatCommand = MCChatCommand(this)

    override fun enable() {
        if (DPUtils.disableIfConfigErrorRes(this, chatChannel, chatChannelMono)) return Unit
        listener = MCChatListener(this)
        TBMCCoreAPI.RegisterEventsForExceptions(listener, plugin)
        TBMCCoreAPI.RegisterEventsForExceptions(
            MCListener(this),
            plugin
        ) //These get undone if restarting/resetting - it will ignore events if disabled
        plugin.manager.registerCommand(mcChatCommand)
        if (state == DPState.RESTARTING_PLUGIN) { //These will only execute if the chat is enabled
            sendStateMessage(
                Color.CYAN,
                "Discord plugin restarted - chat connected."
            ) //Really important to note the chat, hmm
            state = DPState.RUNNING
        } else if (state == DPState.DISABLED_MCCHAT) {
            sendStateMessage(Color.CYAN, "Minecraft chat enabled - chat connected.")
            state = DPState.RUNNING
        } else if (serverUp) {
            sendStateMessage(Color.YELLOW, "Server started after a crash - chat connected.")
            val thr = Throwable("The server shut down unexpectedly. See the log of the previous run for more details.")
            thr.setStackTrace(emptyArray<StackTraceElement>())
            TBMCCoreAPI.SendException("The server crashed!", thr, this)
        } else sendStateMessage(Color.GREEN, "Server started - chat connected.")
        serverUp = true
    }

    override fun disable(): Unit {
        disabling = true
        if (state == DPState.RESTARTING_PLUGIN) sendStateMessage(Color.ORANGE, "Discord plugin restarting")
        else if (state == DPState.RUNNING) {
            sendStateMessage(Color.ORANGE, "Minecraft chat disabled")
            state = DPState.DISABLED_MCCHAT
        } else {
            val kickmsg = if (Bukkit.getOnlinePlayers().isNotEmpty())
                DPUtils.sanitizeString(Bukkit.getOnlinePlayers().joinToString(", ") { it.displayName }) +
                    (if (Bukkit.getOnlinePlayers().size == 1) " was " else " were ") + "thrown out" //TODO: Make configurable
            else ""
            if (state == DPState.RESTARTING_SERVER) sendStateMessage(Color.ORANGE, "Server restarting", kickmsg)
            else if (state == DPState.STOPPING_SERVER) sendStateMessage(Color.RED, "Server stopping", kickmsg)
            else sendStateMessage(Color.GRAY, "Unknown state, please report.")
            //If 'restart' is disabled then this isn't shown even if joinleave is enabled
        }
        serverUp =
            false //Disable even if just the component is disabled because that way it won't falsely report crashes
        if (listener != null) { //Can be null if disabled because of a config error
            listener.stop(true)
        }
        plugin.manager.unregisterCommand(mcChatCommand)
        disabling = false
    }

    /**
     * It will block to make sure all messages are sent
     */
    private fun sendStateMessage(color: Color, message: String) =
        MCChatUtils.forCustomAndAllMCChat({
            it.flatMap { ch ->
                ch.createMessage(
                    EmbedCreateSpec.builder().color(color).title(message).build()
                )
            }
                .onErrorResume { Mono.empty() }
        }, ChannelconBroadcast.RESTART, hookmsg = false).block()

    private fun sendStateMessage(color: Color, message: String, extra: String) =
        MCChatUtils.forCustomAndAllMCChat({
            it.flatMap { ch ->
                ch.createMessage(
                    EmbedCreateSpec.builder().color(color).title(message).description(extra).build()
                )
            }
                .onErrorResume { Mono.empty() }
        }, ChannelconBroadcast.RESTART, hookmsg = false).block()
}