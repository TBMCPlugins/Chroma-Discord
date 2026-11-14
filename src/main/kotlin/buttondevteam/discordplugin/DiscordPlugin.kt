package buttondevteam.discordplugin

import buttondevteam.discordplugin.announcer.AnnouncerModule
import buttondevteam.discordplugin.commands.Command2DC
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule
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
import com.github.shynixn.mccoroutine.bukkit.registerSuspendingEvents
import com.google.common.io.Files
import discord4j.common.util.Snowflake
import discord4j.core.DiscordClientBuilder
import discord4j.core.GatewayDiscordClient
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.`object`.presence.ClientActivity
import discord4j.core.`object`.presence.ClientPresence
import org.bukkit.configuration.file.YamlConfiguration
import reactor.core.publisher.Mono
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.*

@ButtonPlugin.ConfigOpts(disableConfigGen = true)
class DiscordPlugin : ButtonPlugin() {
    companion object DiscordPlugin {
        lateinit var dc: GatewayDiscordClient
        var plugin: buttondevteam.discordplugin.DiscordPlugin? = null
            private set
        var SafeMode = true

        fun getPrefix(): Char = plugin?.prefix ?: '/'

        private var initialized = false
    }

    lateinit var manager: Command2DC
        private set

    private var starting = false

    /**
     * The prefix to use with Discord commands like /role. It only works in the bot channel.
     */
    private val prefix by iConfig.getData("prefix", '/', { str -> (str as String)[0] }, { it.toString() })

    /**
     * The main server where the roles and other information is pulled from. It's automatically set to the first server the bot's invited to.
     */
    var mainServer by iConfig.getData("mainServer", { id -> //It attempts to get the default as well
        if ((id as Long) == 0L) null
        else dc.getGuildById(Snowflake.of(id))
            .onErrorResume({ t ->
                logger.warning("Failed to get guild: " + t.message)
                Mono.empty()
            }).block()
    }, { g -> g?.id?.asLong() ?: 0L }, 0L, true)

    /**
     * The (bot) channel to use for Discord commands like /role.
     */
    val commandChannel by DPUtils.snowflakeData(iConfig, "commandChannel", 0L)

    /**
     * The role that allows using mod-only Discord commands.
     * If empty (&#39;&#39;), then it will only allow for the owner.
     */
    val modRole by DPUtils.roleData(iConfig, "modRole", "Moderator")

    /**
     * The invite link to show by /discord invite. If empty, it defaults to the first invite if the bot has access.
     */
    val inviteLink by iConfig.getData("inviteLink", "")

    override fun pluginEnable(): Unit {
        try {
            logger.info("Initializing...")
            plugin = this
            manager = Command2DC()
            registerCommand(DiscordMCCommand()) //Register so that the restart command works
            var token: String? = null
            val tokenFile = File("TBMC", "Token.txt")
            if (tokenFile.exists()) { //Legacy support
                //noinspection UnstableApiUsage
                token = Files.readFirstLine(tokenFile, StandardCharsets.UTF_8)
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
            starting = true
            val cb = DiscordClientBuilder.create(token).build().gateway()
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
                    .subscribe { this.handleReady(it) } // Take all received GuildCreateEvents and make it a List
            }) /* All guilds have been received, client is fully connected */
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("Failed to enable the Discord plugin!", e, this)
            logger.severe("You may be able to restart the plugin using /discord restart")
            stopStarting()
        }
    }

    private fun stopStarting(): Unit {
        synchronized(this) {
            starting = false
            notifyAll() // TODO
        }
    }

    private fun handleReady(event: List<GuildCreateEvent>) {
        try {
            if (initialized) { //This is not the first ready event
                logger.info("Ready event already handled") //TODO: It should probably handle disconnections
                dc.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft")))
                    ?.subscribe() //Update from the initial presence
                return
            }
            initialized = true
            if (mainServer == null) {
                if (event.isEmpty()) {
                    logger.severe("Main server not found! Invite the bot and do /discord restart")
                    dc.applicationInfo.subscribe({ info -> logger.severe("Click here: https://discordapp.com/oauth2/authorize?client_id=" + info.id.asString() + "&scope=bot&permissions=268509264") })
                    saveConfig() //Put default there
                    return //We should have all guilds by now, no need to retry
                }
                mainServer = event[0].guild
                logger.warning("Main server set to first one: " + mainServer?.name)
            }
            SafeMode = false
            DPUtils.disableIfConfigErrorRes(null, commandChannel, DPUtils.getMessageChannel(commandChannel))
            //Won't disable, just prints the warning here
            if (MinecraftChatModule.state == DPState.STOPPING_SERVER) {
                stopStarting()
                return //Reusing that field to check if stopping while still initializing
            }
            CommonListeners.register(dc.eventDispatcher)
            // TODO: TBMCCoreAPI.RegisterEventsForExceptions
            server.pluginManager.registerSuspendingEvents(MCListener(), this)
            TBMCCoreAPI.RegisterUserClass(DiscordUser::class.java, { DiscordUser() })
            ChromaGamerBase.addConverter {
                when (it) {
                    is DiscordSenderBase -> Optional.of(it.getChromaUser())
                    else -> Optional.empty()
                }
            }
            Component.registerComponent(this, MinecraftChatModule())
            Component.registerComponent(this, ExceptionListenerModule())
            Component.registerComponent(this, GameRoleModule()) //Needs the mainServer to be set
            Component.registerComponent(this, AnnouncerModule())
            Component.registerComponent(this, FunModule())
            MCChatUtils.updatePlayerList() //The MCChatModule is tested to be enabled
            val applicationId = dc.restClient.applicationId.block()
            val guildId = mainServer?.id?.asLong()
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

    override fun pluginPreDisable(): Unit {
        if (MinecraftChatModule.state == DPState.RUNNING) MinecraftChatModule.state = DPState.STOPPING_SERVER
        synchronized(this) {
            if (starting) try {
                wait(10000)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
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