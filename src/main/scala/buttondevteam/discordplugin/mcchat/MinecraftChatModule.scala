package buttondevteam.discordplugin.mcchat

import buttondevteam.core.component.channel.Channel
import buttondevteam.discordplugin.DPUtils.MonoExtensions
import buttondevteam.discordplugin.mcchat.sender.DiscordConnectedPlayer
import buttondevteam.discordplugin.util.DPState
import buttondevteam.discordplugin.{ChannelconBroadcast, DPUtils, DiscordPlugin}
import buttondevteam.lib.architecture.config.IConfigData
import buttondevteam.lib.architecture.{Component, ConfigData}
import buttondevteam.lib.{TBMCCoreAPI, TBMCSystemChatEvent}
import com.google.common.collect.Lists
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.util.Color
import org.bukkit.Bukkit
import reactor.core.publisher.Mono
import reactor.core.scala.publisher.SMono

import java.util
import java.util.stream.Collectors
import java.util.{Objects, UUID}
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsScala

/**
 * Provides Minecraft chat connection to Discord. Commands may be used either in a public chat (limited) or in a DM.
 */
object MinecraftChatModule {
    var state = DPState.RUNNING
}

class MinecraftChatModule extends Component[DiscordPlugin] {
    def getListener: MCChatListener = this.listener

    private var listener: MCChatListener = null
    private[mcchat] var serverWatcher = null
    private var lpInjector = null
    private[mcchat] var disabling = false

    /**
     * A list of commands that can be used in public chats - Warning: Some plugins will treat players as OPs, always test before allowing a command!
     */
    def whitelistedCommands: IConfigData[util.ArrayList[String]] = getConfig.getData("whitelistedCommands",
        Lists.newArrayList("list", "u", "shrug", "tableflip", "unflip", "mwiki", "yeehaw", "lenny", "rp", "plugins"))

    /**
     * The channel to use as the public Minecraft chat - everything public gets broadcasted here
     */
    def chatChannel: IConfigData[Snowflake] = DPUtils.snowflakeData(getConfig, "chatChannel", 0L)

    def chatChannelMono: SMono[MessageChannel] = DPUtils.getMessageChannel(chatChannel.getPath, chatChannel.get)

    /**
     * The channel where the plugin can log when it mutes a player on Discord because of a Minecraft mute
     */
    def modlogChannel: IConfigData[SMono[MessageChannel]] = DPUtils.channelData(getConfig, "modlogChannel")
    /**
     * The plugins to exclude from fake player events used for the 'mcchat' command - some plugins may crash, add them here
     */
    def excludedPlugins: IConfigData[Array[String]] = getConfig.getData("excludedPlugins", Array[String]("ProtocolLib", "LibsDisguises", "JourneyMapServer"))
    /**
     * If this setting is on then players logged in through the 'mcchat' command will be able to teleport using plugin commands.
     * They can then use commands like /tpahere to teleport others to that place.<br />
     * If this is off, then teleporting will have no effect.
     */
    def allowFakePlayerTeleports: IConfigData[Boolean] = getConfig.getData("allowFakePlayerTeleports", false)
    /**
     * If this is on, each chat channel will have a player list in their description.
     * It only gets added if there's no description yet or there are (at least) two lines of "----" following each other.
     * Note that it will replace <b>everything</b> above the first and below the last "----" but it will only detect exactly four dashes.
     * So if you want to use dashes for something else in the description, make sure it's either less or more dashes in one line.
     */
    def showPlayerListOnDC: IConfigData[Boolean] = getConfig.getData("showPlayerListOnDC", true)
    /**
     * This setting controls whether custom chat connections can be <i>created</i> (existing connections will always work).
     * Custom chat connections can be created using the channelcon command and they allow players to display town chat in a Discord channel for example.
     * See the channelcon command for more details.
     */
    def allowCustomChat: IConfigData[Boolean] = getConfig.getData("allowCustomChat", true)
    /**
     * This setting allows you to control if players can DM the bot to log on the server from Discord.
     * This allows them to both chat and perform any command they can in-game.
     */
    def allowPrivateChat: IConfigData[Boolean] = getConfig.getData("allowPrivateChat", true)
    /**
     * If set, message authors appearing on Discord will link to this URL. A 'type' and 'id' parameter will be added with the user's platform (Discord, Minecraft, ...) and ID.
     */
    def profileURL: IConfigData[String] = getConfig.getData("profileURL", "")
    /**
     * Enables support for running vanilla commands through Discord, if you ever need it.
     */
    def enableVanillaCommands: IConfigData[Boolean] = getConfig.getData("enableVanillaCommands", true)
    /**
     * Whether players logged on from Discord (mcchat command) should be recognised by other plugins. Some plugins might break if it's turned off.
     * But it's really hacky.
     */
    final private val addFakePlayersToBukkit = getConfig.getData("addFakePlayersToBukkit", false)
    /**
     * Set by the component to report crashes.
     */
    final private val serverUp = getConfig.getData("serverUp", false)
    final private val mcChatCommand = new MCChatCommand(this)
    final private val channelconCommand = new ChannelconCommand(this)

    override protected def enable(): Unit = {
        if (DPUtils.disableIfConfigErrorRes(this, chatChannel, chatChannelMono)) return ()
        listener = new MCChatListener(this)
        TBMCCoreAPI.RegisterEventsForExceptions(listener, getPlugin)
        TBMCCoreAPI.RegisterEventsForExceptions(new MCListener(this), getPlugin) //These get undone if restarting/resetting - it will ignore events if disabled
        getPlugin.manager.registerCommand(mcChatCommand)
        getPlugin.manager.registerCommand(channelconCommand)
        val chcons = getConfig.getConfig.getConfigurationSection("chcons")
        if (chcons == null) { //Fallback to old place
            getConfig.getConfig.getRoot.getConfigurationSection("chcons")
        }
        if (chcons != null) {
            val chconkeys = chcons.getKeys(false)
            for (chconkey <- chconkeys.asScala) {
                val chcon = chcons.getConfigurationSection(chconkey)
                val mcch = Channel.getChannels.filter((ch: Channel) => ch.getIdentifier == chcon.getString("mcchid")).findAny
                val ch = DiscordPlugin.dc.getChannelById(Snowflake.of(chcon.getLong("chid"))).block
                val did = chcon.getLong("did")
                val user = DiscordPlugin.dc.getUserById(Snowflake.of(did)).block
                val groupid = chcon.getString("groupid")
                val toggles = chcon.getInt("toggles")
                val brtoggles = chcon.getStringList("brtoggles")
                if (mcch.isPresent && ch != null && user != null && groupid != null) {
                    Bukkit.getScheduler.runTask(getPlugin, () => { //<-- Needed because of occasional ConcurrentModificationExceptions when creating the player (PermissibleBase)
                        val dcp = DiscordConnectedPlayer.create(user, ch.asInstanceOf[MessageChannel],
                            UUID.fromString(chcon.getString("mcuid")), chcon.getString("mcname"), this)
                        MCChatCustom.addCustomChat(ch.asInstanceOf[MessageChannel], groupid, mcch.get, user, dcp, toggles,
                            brtoggles.asScala.map(TBMCSystemChatEvent.BroadcastTarget.get).filter(Objects.nonNull).toSet)
                        ()
                    })
                }
            }
        }
        // TODO: LPInjector
        if (addFakePlayersToBukkit.get) try {
            // TODO: Fake players
        } catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("Failed to hack the server (object)! Disable addFakePlayersToBukkit in the config.", e, this)
        }
        if (MinecraftChatModule.state eq DPState.RESTARTING_PLUGIN) { //These will only execute if the chat is enabled
            sendStateMessage(Color.CYAN, "Discord plugin restarted - chat connected.") //Really important to note the chat, hmm
            MinecraftChatModule.state = DPState.RUNNING
        }
        else if (MinecraftChatModule.state eq DPState.DISABLED_MCCHAT) {
            sendStateMessage(Color.CYAN, "Minecraft chat enabled - chat connected.")
            MinecraftChatModule.state = DPState.RUNNING
        }
        else if (serverUp.get) {
            sendStateMessage(Color.YELLOW, "Server started after a crash - chat connected.")
            val thr = new Throwable("The server shut down unexpectedly. See the log of the previous run for more details.")
            thr.setStackTrace(new Array[StackTraceElement](0))
            TBMCCoreAPI.SendException("The server crashed!", thr, this)
        }
        else sendStateMessage(Color.GREEN, "Server started - chat connected.")
        serverUp.set(true)
    }

    override protected def disable(): Unit = {
        disabling = true
        if (MinecraftChatModule.state eq DPState.RESTARTING_PLUGIN) sendStateMessage(Color.ORANGE, "Discord plugin restarting")
        else if (MinecraftChatModule.state eq DPState.RUNNING) {
            sendStateMessage(Color.ORANGE, "Minecraft chat disabled")
            MinecraftChatModule.state = DPState.DISABLED_MCCHAT
        }
        else {
            val kickmsg = if (Bukkit.getOnlinePlayers.size > 0)
                DPUtils.sanitizeString(Bukkit.getOnlinePlayers.asScala.map(_.getDisplayName).mkString(", ")) +
                    (if (Bukkit.getOnlinePlayers.size == 1) " was " else " were ") + "thrown out" //TODO: Make configurable
            else ""
            if (MinecraftChatModule.state eq DPState.RESTARTING_SERVER) sendStateMessage(Color.ORANGE, "Server restarting", kickmsg)
            else if (MinecraftChatModule.state eq DPState.STOPPING_SERVER) sendStateMessage(Color.RED, "Server stopping", kickmsg)
            else sendStateMessage(Color.GRAY, "Unknown state, please report.")
            //If 'restart' is disabled then this isn't shown even if joinleave is enabled}
        }
        serverUp.set(false) //Disable even if just the component is disabled because that way it won't falsely report crashes
        try //If it's not enabled it won't do anything
            if (serverWatcher != null) {
                // TODO: ServerWatcher
            }
        catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("Failed to restore the server object!", e, this)
        }
        val chcons = MCChatCustom.getCustomChats
        val chconsc = getConfig.getConfig.createSection("chcons")
        for (chcon <- chcons) {
            val chconc = chconsc.createSection(chcon.channel.getId.asString)
            chconc.set("mcchid", chcon.mcchannel.getIdentifier)
            chconc.set("chid", chcon.channel.getId.asLong)
            chconc.set("did", chcon.dcUser.getId.asLong)
            chconc.set("mcuid", chcon.dcp.getUniqueId.toString)
            chconc.set("mcname", chcon.dcp.getName)
            chconc.set("groupid", chcon.groupID)
            chconc.set("toggles", chcon.toggles)
            chconc.set("brtoggles", chcon.brtoggles.map(_.getName).toList)
        }
        if (listener != null) { //Can be null if disabled because of a config error
            listener.stop(true)
        }
        getPlugin.manager.unregisterCommand(mcChatCommand)
        getPlugin.manager.unregisterCommand(channelconCommand)
        disabling = false
    }

    /**
     * It will block to make sure all messages are sent
     */
    private def sendStateMessage(color: Color, message: String) =
        MCChatUtils.forCustomAndAllMCChat(_.flatMap(ch => ch.createMessage(
            EmbedCreateSpec.builder().color(color).title(message).build()).^^()
                .onErrorResume(_ => SMono.empty)
        ), ChannelconBroadcast.RESTART, hookmsg = false).block()

    private def sendStateMessage(color: Color, message: String, extra: String) =
        MCChatUtils.forCustomAndAllMCChat(_.flatMap(ch => ch.createMessage(
            EmbedCreateSpec.builder().color(color).title(message).description(extra).build()).^^()
                .onErrorResume(_ => SMono.empty)
        ), ChannelconBroadcast.RESTART, hookmsg = false).block()
}