package buttondevteam.discordplugin

import buttondevteam.discordplugin.DiscordPlugin.dc
import buttondevteam.discordplugin.announcer.AnnouncerModule
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule
import buttondevteam.discordplugin.commands.*
import buttondevteam.discordplugin.exceptions.ExceptionListenerModule
import buttondevteam.discordplugin.fun.FunModule
import buttondevteam.discordplugin.listeners.{CommonListeners, MCListener}
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.mcchat.sender.{DiscordPlayer, DiscordSenderBase}
import buttondevteam.discordplugin.mccommands.DiscordMCCommand
import buttondevteam.discordplugin.role.GameRoleModule
import buttondevteam.discordplugin.util.{DPState, Timings}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.*
import buttondevteam.lib.player.ChromaGamerBase
import com.google.common.io.Files
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.{ApplicationInfo, Guild, Role}
import discord4j.core.`object`.presence.{Activity, ClientActivity, ClientPresence, Presence}
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.{DiscordClientBuilder, GatewayDiscordClient}
import discord4j.gateway.ShardInfo
import discord4j.rest.interaction.Interactions
import discord4j.store.jdk.JdkStoreService
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.mockito.internal.util.MockUtil
import reactor.core.Disposable
import reactor.core.publisher.Mono

import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Optional
import scala.jdk.OptionConverters.*

@ButtonPlugin.ConfigOpts(disableConfigGen = true) object DiscordPlugin {
    private[discordplugin] var dc: GatewayDiscordClient = null
    private[discordplugin] var plugin: DiscordPlugin = null
    private[discordplugin] var SafeMode = true

    def getPrefix: Char = {
        if (plugin == null) '/'
        else plugin.prefix.get
    }

    private[discordplugin] var mainServer: Guild = null
    private[discordplugin] val DELIVERED_REACTION = ReactionEmoji.unicode("✅")
}

@ButtonPlugin.ConfigOpts(disableConfigGen = true) class DiscordPlugin extends ButtonPlugin {
    private var _manager: Command2DC = null

    def manager: Command2DC = _manager

    private var starting = false
    /**
     * The prefix to use with Discord commands like /role. It only works in the bot channel.
     */
    final private val prefix = getIConfig.getData("prefix", '/', (str: Any) => str.asInstanceOf[String].charAt(0), (_: Char).toString)

    /**
     * The main server where the roles and other information is pulled from. It's automatically set to the first server the bot's invited to.
     */
    private def mainServer = getIConfig.getData("mainServer", id => { //It attempts to get the default as well
        if (id.asInstanceOf[Long] == 0L) Option.empty
        else DiscordPlugin.dc.getGuildById(Snowflake.of(id.asInstanceOf[Long]))
            .onErrorResume(t => {
                getLogger.warning("Failed to get guild: " + t.getMessage)
                Mono.empty
            }).blockOptional().toScala
    }, g => g.map(_.getId.asLong).getOrElse(0L), 0L, true)

    /**
     * The (bot) channel to use for Discord commands like /role.
     */
    var commandChannel: ConfigData[Snowflake] = DPUtils.snowflakeData(getIConfig, "commandChannel", 0L)
    /**
     * The role that allows using mod-only Discord commands.
     * If empty (&#39;&#39;), then it will only allow for the owner.
     */
    var modRole: ConfigData[Mono[Role]] = null
    /**
     * The invite link to show by /discord invite. If empty, it defaults to the first invite if the bot has access.
     */
    var inviteLink: ConfigData[String] = getIConfig.getData("inviteLink", "")

    private def setupConfig(): Unit = modRole = DPUtils.roleData(getIConfig, "modRole", "Moderator")

    override def onLoad(): Unit = { //Needed by ServerWatcher
        val thread = Thread.currentThread
        val cl = thread.getContextClassLoader
        thread.setContextClassLoader(getClassLoader)
        MockUtil.isMock(null) //Load MockUtil to load Mockito plugins
        thread.setContextClassLoader(cl)
        getLogger.info("Load complete")
    }

    override def pluginEnable(): Unit = try {
        getLogger.info("Initializing...")
        DiscordPlugin.plugin = this
        _manager = new Command2DC
        registerCommand(new DiscordMCCommand) //Register so that the restart command works
        var token: String = null
        val tokenFile = new File("TBMC", "Token.txt")
        if (tokenFile.exists) { //Legacy support
            //noinspection UnstableApiUsage
            token = Files.readFirstLine(tokenFile, StandardCharsets.UTF_8)
        }
        else {
            val privateFile = new File(getDataFolder, "private.yml")
            val conf = YamlConfiguration.loadConfiguration(privateFile)
            token = conf.getString("token")
            if (token == null || token.equalsIgnoreCase("Token goes here")) {
                conf.set("token", "Token goes here")
                conf.save(privateFile)
                getLogger.severe("Token not found! Please set it in private.yml then do /discord restart")
                getLogger.severe("You need to have a bot account to use with your server.")
                getLogger.severe("If you don't have one, go to https://discordapp.com/developers/applications/ and create an application, then create a bot for it and copy the bot token.")
                return ()
            }
        }
        starting = true
        val cb = DiscordClientBuilder.create(token).build.gateway
        cb.setInitialPresence((si: ShardInfo) => ClientPresence.doNotDisturb(ClientActivity.playing("booting")))
        cb.login.doOnError((t: Throwable) => {
            def foo(t: Throwable): Unit = {
                stopStarting()
            }

            foo(t)
        }).subscribe((dc: GatewayDiscordClient) => {
            DiscordPlugin.dc = dc //Set to gateway client
            dc.on(classOf[ReadyEvent]).map(_.getGuilds.size).flatMap(dc.on(classOf[GuildCreateEvent]).take(_).collectList)
                .doOnError(_ => stopStarting()).subscribe(this.handleReady _) // Take all received GuildCreateEvents and make it a List
            ()
        }) /* All guilds have been received, client is fully connected */
    } catch {
        case e: Exception =>
            TBMCCoreAPI.SendException("Failed to enable the Discord plugin!", e, this)
            getLogger.severe("You may be able to restart the plugin using /discord restart")
            stopStarting()
    }

    private def stopStarting(): Unit = {
        this synchronized {
            starting = false
            notifyAll()
        }
    }

    private def handleReady(event: java.util.List[GuildCreateEvent]): Unit = { //System.out.println("Got ready event");
        try {
            if (DiscordPlugin.mainServer != null) { //This is not the first ready event
                getLogger.info("Ready event already handled") //TODO: It should probably handle disconnections
                DiscordPlugin.dc.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft"))).subscribe //Update from the initial presence
                return ()
            }
            DiscordPlugin.mainServer = mainServer.get.orNull //Shouldn't change afterwards
            if (DiscordPlugin.mainServer == null) {
                if (event.size == 0) {
                    getLogger.severe("Main server not found! Invite the bot and do /discord restart")
                    DiscordPlugin.dc.getApplicationInfo.subscribe((info: ApplicationInfo) => getLogger.severe("Click here: https://discordapp.com/oauth2/authorize?client_id=" + info.getId.asString + "&scope=bot&permissions=268509264"))
                    saveConfig() //Put default there
                    return () //We should have all guilds by now, no need to retry
                }
                DiscordPlugin.mainServer = event.get(0).getGuild
                getLogger.warning("Main server set to first one: " + DiscordPlugin.mainServer.getName)
                mainServer.set(Option(DiscordPlugin.mainServer)) //Save in config
            }
            DiscordPlugin.SafeMode = false
            setupConfig()
            DPUtils.disableIfConfigErrorRes(null, commandChannel, DPUtils.getMessageChannel(commandChannel))
            //Won't disable, just prints the warning here
            if (MinecraftChatModule.state eq DPState.STOPPING_SERVER) {
                stopStarting()
                return () //Reusing that field to check if stopping while still initializing
            }
            CommonListeners.register(DiscordPlugin.dc.getEventDispatcher)
            TBMCCoreAPI.RegisterEventsForExceptions(new MCListener, this)
            TBMCCoreAPI.RegisterUserClass(classOf[DiscordPlayer], () => new DiscordPlayer)
            ChromaGamerBase.addConverter {
                case dsender: DiscordSenderBase => Some(dsender.getChromaUser).toJava
                case _ => None.toJava
            }
            ChromaBot.enabled = true //Initialize ChromaBot
            Component.registerComponent(this, new GeneralEventBroadcasterModule)
            Component.registerComponent(this, new MinecraftChatModule)
            Component.registerComponent(this, new ExceptionListenerModule)
            Component.registerComponent(this, new GameRoleModule) //Needs the mainServer to be set
            Component.registerComponent(this, new AnnouncerModule)
            Component.registerComponent(this, new FunModule)
            ChromaBot.updatePlayerList() //The MCChatModule is tested to be enabled
            val applicationId = dc.getRestClient.getApplicationId.block()
            val guildId = Some(DiscordPlugin.mainServer.getId.asLong())
            manager.registerCommand(new VersionCommand, applicationId, guildId)
            manager.registerCommand(new UserinfoCommand, applicationId, guildId)
            manager.registerCommand(new HelpCommand, applicationId, guildId)
            manager.registerCommand(new DebugCommand, applicationId, guildId)
            manager.registerCommand(new ConnectCommand, applicationId, guildId)
            TBMCCoreAPI.SendUnsentExceptions()
            TBMCCoreAPI.SendUnsentDebugMessages()
            // TODO: Removed log watcher (responsible for detecting restarts)
            if (!TBMCCoreAPI.IsTestServer) DiscordPlugin.dc.updatePresence(ClientPresence.online(ClientActivity.playing("Minecraft"))).subscribe()
            else DiscordPlugin.dc.updatePresence(ClientPresence.online(ClientActivity.playing("testing"))).subscribe()
            getLogger.info("Loaded!")
        } catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("An error occurred while enabling DiscordPlugin!", e, this)
        }
        stopStarting()
    }

    override def pluginPreDisable(): Unit = {
        if (MinecraftChatModule.state eq DPState.RUNNING) MinecraftChatModule.state = DPState.STOPPING_SERVER
        this synchronized {
            if (starting) try wait(10000)
            catch {
                case e: InterruptedException =>
                    e.printStackTrace()
            }
        }
        if (!ChromaBot.enabled) return () //Failed to load
        val timings = new Timings
        timings.printElapsed("Disable start")
        timings.printElapsed("Updating player list")
        ChromaBot.updatePlayerList()
        timings.printElapsed("Done")
    }

    override def pluginDisable(): Unit = {
        val timings = new Timings
        timings.printElapsed("Actual disable start (logout)")
        if (!ChromaBot.enabled) return ()
        try {
            DiscordPlugin.SafeMode = true // Stop interacting with Discord
            ChromaBot.enabled = false
            timings.printElapsed("Logging out...")
            DiscordPlugin.dc.logout.block
            DiscordPlugin.mainServer = null //Allow ReadyEvent again
        } catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("An error occured while disabling DiscordPlugin!", e, this)
        }
    }
}