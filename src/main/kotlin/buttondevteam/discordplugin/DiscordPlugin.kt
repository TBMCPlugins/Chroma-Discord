package buttondevteam.discordplugin

import buttondevteam.discordplugin.DPUtils.channelData
import buttondevteam.discordplugin.DPUtils.guildData
import buttondevteam.discordplugin.DPUtils.roleData
import buttondevteam.discordplugin.commands.*
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule
import buttondevteam.discordplugin.`fun`.FunModule
import buttondevteam.discordplugin.listeners.CommonListeners
import buttondevteam.discordplugin.listeners.MCListener
import buttondevteam.discordplugin.mcchat.MCChatUtils
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.mcchat.sender.DiscordSenderBase
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.discordplugin.mccommands.DiscordMCCommand
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.DPState
import buttondevteam.discordplugin.util.Timings
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.ButtonPlugin
import buttondevteam.lib.architecture.Component
import buttondevteam.lib.player.ChromaGamerBase
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.common.io.Files
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import discord4j.gateway.intent.Intent
import discord4j.gateway.intent.IntentSet
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

@ButtonPlugin.ConfigOpts(disableConfigGen = true)
class DiscordPlugin : ButtonPlugin() {
    companion object DiscordPlugin {
        lateinit var dc: GatewayDiscordClient
        lateinit var plugin: buttondevteam.discordplugin.DiscordPlugin
            private set
        var SafeMode = true

        fun getPrefix(): Char = plugin.prefix

        private var initialized = false
    }

    lateinit var manager: Command2DC
        private set

    /**
     * The prefix to use with Discord commands like /role. It only works in the bot channel.
     */
    private val prefix by iConfig.getData("prefix", '/', { str -> (str as String)[0] }, { it.toString() })

    /**
     * The main server where the roles and other information is pulled from. It's automatically set to the first server the bot's invited to.
     */
    var mainServerConfig = iConfig.guildData("mainServer")

    /**
     * The main server where the roles and other information is pulled from. It's automatically set to the first server the bot's invited to.
     */
    lateinit var mainServer: Guild private set

    /**
     * The (bot) channel to use for Discord commands like /role.
     */
    val commandChannel = iConfig.channelData("commandChannel")

    /**
     * The role that allows using mod-only Discord commands.
     * If empty (&#39;&#39;), then it will only allow for the owner.
     */
    val modRole = iConfig.roleData("modRole")

    /**
     * The invite link to show by /discord invite. If empty, it defaults to the first invite if the bot has access.
     */
    val inviteLink by iConfig.getData("inviteLink", "")

    override fun pluginEnable() {
        try {
            logger.info("Initializing...")
            plugin = this
            manager = Command2DC()
            registerCommand(DiscordMCCommand()) //Register so that the restart command works
            var token: String? = null
            val tokenFile = File("TBMC", "Token.txt")
            if (tokenFile.exists()) { //Legacy support
                token = Files.asCharSource(tokenFile, StandardCharsets.UTF_8).readFirstLine()
            }
            if (token == null) {
                val privateFile = File(dataFolder, "private.yml")
                val conf = YamlConfiguration.loadConfiguration(privateFile)
                token = conf.getString("token")
                if (token == null || token.equals("Token goes here", true)) {
                    conf.set("token", "Token goes here")
                    conf.save(privateFile)
                    logger.severe("Token not found! Please set it in private.yml then do /discord restart")
                    logger.severe("You need to have a bot account to use with your server.")
                    logger.severe("If you don't have one, go to https://discordapp.com/developers/applications/ and create an application, then create a bot for it and copy the bot token.")
                    return
                }
            }
            val cb = DiscordClientBuilder.create(token).build().gateway()
            cb.setEnabledIntents(IntentSet.nonPrivileged().or(IntentSet.of(Intent.MESSAGE_CONTENT)))
            cb.setInitialPresence({ si -> ClientPresence.doNotDisturb(ClientActivity.playing("booting")) })
            cb.login().doOnError({ t ->
                fun foo(t: Throwable): Unit {
                    stopStarting()
                }

                foo(t)
            }).subscribe({ dc ->
                DiscordPlugin.dc = dc //Set to gateway client
                dc.on(ReadyEvent::class.java).map { it.guilds.size }
                    .flatMap { dc.on(GuildCreateEvent::class.java).take(it.toLong()).collectList() }
                    .doOnError { _ -> stopStarting() }
                    .subscribe { launch { this@DiscordPlugin.handleReady(it) } } // Take all received GuildCreateEvents and make it a List
            }) /* All guilds have been received, client is fully connected */
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("Failed to enable the Discord plugin!", e, this)
            logger.severe("You may be able to restart the plugin using /discord restart")
            stopStarting()
        }
    }

    private fun stopStarting() {
        // TODO: Cancel the coroutines
    }

    private suspend fun handleReady(event: List<GuildCreateEvent>) {
        try {
            if (initialized) { //This is not the first ready event
                logger.info("Ready event already handled") //TODO: It should probably handle disconnections
                dc.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft")))
                    .awaitFirstOrNull() //Update from the initial presence
                return
            }
            initialized = true
            if (mainServerConfig.getEntity() == null) {
                if (event.isEmpty()) {
                    logger.severe("Main server not found! Invite the bot and do /discord restart")
                    dc.applicationInfo.awaitFirstOrNull()?.let { info ->
                        logger.severe("Click here: https://discordapp.com/oauth2/authorize?client_id=${info.id.asString()}&scope=bot&permissions=268509264")
                    }
                    dc.applicationInfo.subscribe({ info ->  })
                    saveConfig() //Put default there
                    return //We should have all guilds by now, no need to retry
                }
                mainServerConfig.id = event[0].guild.id
                val server = mainServerConfig.getEntity()
                if (server == null) {
                    logger.severe("Internal error: got servers but still couldn't fetch the main server, please report.")
                    return
                }
                logger.warning("Main server set to first one: " + server.name)
            }
            mainServer = mainServerConfig.getEntity()!!
            SafeMode = false
            DPUtils.disableIfConfigError(null, commandChannel)
            //Won't disable, just prints the warning here
            if (MinecraftChatModule.state == DPState.STOPPING_SERVER) {
                stopStarting()
                return //Reusing that field to check if stopping while still initializing
            }
            CommonListeners.register(dc.eventDispatcher)
            // TODO: TBMCCoreAPI.RegisterEventsForExceptions
            server.pluginManager.registerSuspendingEvents(MCListener(), this)
            TBMCCoreAPI.RegisterUserClass(DiscordUser::class.java) { DiscordUser() }
            ChromaGamerBase.addConverter {
                when (it) {
                    is DiscordSenderBase -> Optional.of(it.getChromaUser())
                    else -> Optional.empty()
                }
            }
            Component.registerComponent(this, MinecraftChatModule())
            Component.registerComponent(this, ExceptionListenerModule())
            Component.registerComponent(this, GameRoleModule()) //Needs the mainServer to be set
            Component.registerComponent(this, FunModule())
            MCChatUtils.updatePlayerList() //The MCChatModule is tested to be enabled
            val applicationId = dc.restClient.applicationId.awaitFirst()
            val guildId = mainServer.id.asLong()
            manager.registerCommand(VersionCommand(), applicationId, guildId)
            manager.registerCommand(UserinfoCommand(), applicationId, guildId)
            manager.registerCommand(HelpCommand(), applicationId, guildId)
            manager.registerCommand(DebugCommand(), applicationId, guildId)
            manager.registerCommand(ConnectCommand(), applicationId, guildId)
            TBMCCoreAPI.SendUnsentExceptions()
            TBMCCoreAPI.SendUnsentDebugMessages()
            // TODO: Removed log watcher (responsible for detecting restarts)
            if (!TBMCCoreAPI.IsTestServer()) dc.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft")))
                .subscribe()
            else dc.updatePresence(ClientPresence.online(ClientActivity.playing("testing"))).subscribe()
            logger.info("Loaded!")
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("An error occurred while enabling DiscordPlugin!", e, this)
        }
        stopStarting()
    }

    override fun pluginPreDisable() {
        if (MinecraftChatModule.state == DPState.RUNNING) MinecraftChatModule.state = DPState.STOPPING_SERVER
        // TODO: Test if the deafult coroutine disposal strategy works well with Discord
        if (!initialized) return //Failed to load
        val timings = Timings()
        timings.printElapsed("Disable start")
        timings.printElapsed("Updating player list")
        MCChatUtils.updatePlayerList()
        timings.printElapsed("Done")
    }

    override fun pluginDisable() {
        val timings = Timings()
        timings.printElapsed("Actual disable start (logout)")
        if (!initialized) return
        try {
            SafeMode = true // Stop interacting with Discord
            timings.printElapsed("Logging out...")
            dc.logout().block()
            initialized = false //Allow ReadyEvent again
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e, this)
        }
    }
}