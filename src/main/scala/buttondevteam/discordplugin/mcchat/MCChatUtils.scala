package buttondevteam.discordplugin.mcchat

import buttondevteam.core.{ComponentManager, MainPlugin, component}
import buttondevteam.discordplugin.*
import buttondevteam.discordplugin.ChannelconBroadcast.ChannelconBroadcast
import buttondevteam.discordplugin.DPUtils.SpecExtensions
import buttondevteam.discordplugin.broadcaster.GeneralEventBroadcasterModule
import buttondevteam.discordplugin.mcchat.MCChatCustom.CustomLMD
import buttondevteam.discordplugin.mcchat.sender.{DiscordConnectedPlayer, DiscordPlayerSender, DiscordSender, DiscordSenderBase}
import buttondevteam.lib.player.{ChromaGamerBase, TBMCPlayerBase}
import buttondevteam.lib.{TBMCCoreAPI, TBMCSystemChatEvent}
import com.google.common.collect.Sets
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.{Channel, MessageChannel, PrivateChannel, TextChannel}
import discord4j.core.`object`.entity.{Message, User}
import discord4j.core.spec.legacy.LegacyTextChannelEditSpec
import io.netty.util.collection.LongObjectHashMap
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.player.{AsyncPlayerPreLoginEvent, PlayerJoinEvent, PlayerLoginEvent, PlayerQuitEvent}
import org.bukkit.plugin.AuthorNagException
import reactor.core.publisher.{Flux, Mono}

import java.net.InetAddress
import java.util
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import java.util.stream.Collectors
import javax.annotation.Nullable
import scala.collection.{concurrent, mutable}
import scala.collection.convert.ImplicitConversions.`map AsJavaMap`
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.{CollectionHasAsScala, SeqHasAsJava}
import scala.jdk.javaapi.CollectionConverters.asScala
import scala.jdk.javaapi.OptionConverters.*

object MCChatUtils {
    /**
     * May contain P&lt;DiscordID&gt; as key for public chat
     */
    val UnconnectedSenders = asScala(new ConcurrentHashMap[String, ConcurrentHashMap[Snowflake, DiscordSender]])
    val ConnectedSenders = asScala(new ConcurrentHashMap[String, ConcurrentHashMap[Snowflake, DiscordConnectedPlayer]])
    val OnlineSenders = asScala(new ConcurrentHashMap[String, ConcurrentHashMap[Snowflake, DiscordPlayerSender]])
    val LoggedInPlayers = asScala(new ConcurrentHashMap[UUID, DiscordConnectedPlayer])
    @Nullable private[mcchat] var lastmsgdata: MCChatUtils.LastMsgData = null
    private[mcchat] val lastmsgfromd = new LongObjectHashMap[Message] // Last message sent by a Discord user, used for clearing checkmarks
    private var module: MinecraftChatModule = null
    private val staticExcludedPlugins: concurrent.Map[Class[_ <: Event], util.HashSet[String]] = concurrent.TrieMap()

    val broadcastedMessages: mutable.Map[String, Long] = mutable.Map()

    def updatePlayerList(): Unit = {
        val mod = getModule
        if (mod == null || !mod.showPlayerListOnDC.get) return ()
        if (lastmsgdata != null) updatePL(lastmsgdata)
        MCChatCustom.lastmsgCustom.foreach(MCChatUtils.updatePL)
    }

    private def notEnabled = (module == null || !module.disabling) && getModule == null //Allow using things while disabling the module

    private def getModule = {
        if (module == null || !module.isEnabled) module = ComponentManager.getIfEnabled(classOf[MinecraftChatModule])
        //If disabled, it will try to get it again because another instance may be enabled - useful for /discord restart
        module
    }

    private def updatePL(lmd: MCChatUtils.LastMsgData): Unit = {
        if (!lmd.channel.isInstanceOf[TextChannel]) {
            TBMCCoreAPI.SendException("Failed to update player list for channel " + lmd.channel.getId, new Exception("The channel isn't a (guild) text channel."), getModule)
            return ()
        }
        var topic = lmd.channel.asInstanceOf[TextChannel].getTopic.orElse("")
        if (topic.isEmpty) topic = ".\n----\nMinecraft chat\n----\n."
        val s = topic.split("\\n----\\n")
        if (s.length < 3) return ()
        var gid: String = null
        lmd match {
            case clmd: CustomLMD => gid = clmd.groupID
            case _ => //If we're not using a custom chat then it's either can ("everyone") or can't (null) see at most
                gid = buttondevteam.core.component.channel.Channel.GROUP_EVERYONE // (Though it's a public chat then rn)
        }
        val C = new AtomicInteger
        s(s.length - 1) = "Players: " + Bukkit.getOnlinePlayers.stream.filter(p => if (lmd.mcchannel == null) {
            gid == buttondevteam.core.component.channel.Channel.GROUP_EVERYONE //If null, allow if public (custom chats will have their channel stored anyway)
        }
        else {
            gid == lmd.mcchannel.getGroupID(ChromaGamerBase.getFromSender(p))
        }
        ).filter(MCChatUtils.checkEssentials) //If they can see it
            .filter(_ => C.incrementAndGet > 0) //Always true
            .map((p) => DPUtils.sanitizeString(p.getDisplayName)).collect(Collectors.joining(", "))
        s(0) = s"$C player${if (C.get != 1) "s" else ""} online"
        lmd.channel.asInstanceOf[TextChannel].edit().withTopic(String.join("\n----\n", s: _*)).withReason("Player list update").subscribe() //Don't wait
    }

    private[mcchat] def checkEssentials(p: Player): Boolean = {
        val ess = MainPlugin.ess
        if (ess == null) return true
        !ess.getUser(p).isHidden
    }

    def addSenderTo[T <: DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], user: User, sender: T): T =
        addSenderTo(senders, user.getId.asString, sender)

    def addSenderTo[T <: DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], did: String, sender: T): T = {
        val mapOpt = senders.get(did)
        val map = if (mapOpt.isEmpty) new ConcurrentHashMap[Snowflake, T] else mapOpt.get
        map.put(sender.getChannel.getId, sender)
        senders.put(did, map)
        sender
    }

    def getSenderFrom[T <: DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], channel: Snowflake, user: User): T = {
        val mapOpt = senders.get(user.getId.asString)
        if (mapOpt.nonEmpty) mapOpt.get.get(channel)
        else null.asInstanceOf
    }

    def removeSenderFrom[T <: DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], channel: Snowflake, user: User): T = {
        val mapOpt = senders.get(user.getId.asString)
        if (mapOpt.nonEmpty) mapOpt.get.remove(channel)
        else null.asInstanceOf
    }

    def forPublicPrivateChat(action: Mono[MessageChannel] => Mono[_]): Mono[_] = {
        if (notEnabled) return Mono.empty
        val list = MCChatPrivate.lastmsgPerUser.map(data => action(Mono.just(data.channel)))
            .prepend(action(module.chatChannelMono))
        Mono.whenDelayError(list.asJava)
    }

    /**
     * For custom and all MC chat
     *
     * @param action  The action to act (cannot complete empty)
     * @param toggle  The toggle to check
     * @param hookmsg Whether the message is also sent from the hook
     */
    def forCustomAndAllMCChat(action: Mono[MessageChannel] => Mono[_], @Nullable toggle: ChannelconBroadcast, hookmsg: Boolean): Mono[_] = {
        if (notEnabled) return Mono.empty
        val list = List(if (!GeneralEventBroadcasterModule.isHooked || !hookmsg)
            forPublicPrivateChat(action) else Mono.empty) ++
            (if (toggle == null) MCChatCustom.lastmsgCustom
            else MCChatCustom.lastmsgCustom.filter(cc => (cc.toggles & (1 << toggle.id)) != 0))
                .map(_.channel).map(Mono.just).map(action)
        Mono.whenDelayError(list.asJava)
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action The action to do
     * @param permUser The user to check perms of or null to send to all that has it toggled
     * @param toggle The toggle to check or null to send to all allowed
     */
    def forAllowedCustomMCChat(action: Mono[MessageChannel] => Mono[_], @Nullable permUser: ChromaGamerBase, @Nullable toggle: ChannelconBroadcast): Mono[_] = {
        if (notEnabled) return Mono.empty
        val st = MCChatCustom.lastmsgCustom.filter(clmd => { //new TBMCChannelConnectFakeEvent(sender, clmd.mcchannel).shouldSendTo(clmd.dcp) - Thought it was this simple hehe - Wait, it *should* be this simple
            if (toggle != null && ((clmd.toggles & (1 << toggle.id)) == 0)) false //If null then allow
            else if (permUser == null) true
            else clmd.groupID.equals(clmd.mcchannel.getGroupID(permUser))
        }).map(cc => action.apply(Mono.just(cc.channel))) //TODO: Send error messages on channel connect
        //Mono.whenDelayError((() => st.iterator).asInstanceOf[java.lang.Iterable[Mono[_]]]) //Can't convert as an iterator or inside the stream, but I can convert it as a stream
        Mono.whenDelayError(st.asJava)
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action  The action to do
     * @param permUser  The user to check perms of or null to send to all that has it toggled
     * @param toggle  The toggle to check or null to send to all allowed
     * @param hookmsg Whether the message is also sent from the hook
     */
    def forAllowedCustomAndAllMCChat(action: Mono[MessageChannel] => Mono[_], @Nullable permUser: ChromaGamerBase, @Nullable toggle: ChannelconBroadcast, hookmsg: Boolean): Mono[_] = {
        if (notEnabled) return Mono.empty
        val cc = forAllowedCustomMCChat(action, permUser, toggle)
        if (!GeneralEventBroadcasterModule.isHooked || !hookmsg) return Mono.whenDelayError(forPublicPrivateChat(action), cc)
        Mono.whenDelayError(cc)
    }

    def send(message: String): Mono[MessageChannel] => Mono[_] = _.flatMap((mc: MessageChannel) => {
        resetLastMessage(mc)
        mc.createMessage(DPUtils.sanitizeString(message))
    })

    def forAllowedMCChat(action: Mono[MessageChannel] => Mono[_], event: TBMCSystemChatEvent): Mono[_] = {
        if (notEnabled) return Mono.empty
        val list = new ListBuffer[Mono[_]]
        if (event.getChannel.isGlobal) list.append(action(module.chatChannelMono))
        for (data <- MCChatPrivate.lastmsgPerUser)
            if (event.shouldSendTo(getSender(data.channel.getId, data.user)))
                list.append(action(Mono.just(data.channel))) //TODO: Only store ID?
        MCChatCustom.lastmsgCustom.filter(clmd =>
            clmd.brtoggles.contains(event.getTarget) && event.shouldSendTo(clmd.dcp.getChromaUser))
            .map(clmd => action(Mono.just(clmd.channel))).foreach(elem => {
            list.append(elem)
            ()
        })
        Mono.whenDelayError(list.asJava)
    }

    /**
     * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
     */
    private[mcchat] def getSender(channel: Snowflake, author: User): DiscordSenderBase = { //noinspection OptionalGetWithoutIsPresent
        Option(getSenderFrom(OnlineSenders, channel, author)) // Find first non-null
            .orElse(Option(getSenderFrom(ConnectedSenders, channel, author))) // This doesn't support the public chat, but it'll always return null for it
            .orElse(Option(getSenderFrom(UnconnectedSenders, channel, author))) //
            .orElse(Option(addSenderTo(UnconnectedSenders, author,
                new DiscordSender(author, DiscordPlugin.dc.getChannelById(channel).block().asInstanceOf[MessageChannel]))))
            .get
    }

    /**
     * Resets the last message, so it will start a new one instead of appending to it.
     * This is used when someone (even the bot) sends a message to the channel.
     *
     * @param channel The channel to reset in - the process is slightly different for the public, private and custom chats
     */
    def resetLastMessage(channel: Channel): Unit = {
        if (notEnabled) return ()
        if (channel.getId.asLong == module.chatChannel.get.asLong) {
            if (lastmsgdata == null) lastmsgdata = new MCChatUtils.LastMsgData(module.chatChannelMono.block(), null)
            else lastmsgdata.message = null
            return ()
        } // Don't set the whole object to null, the player and channel information should be preserved
        val channelData = if (channel.isInstanceOf[PrivateChannel]) MCChatPrivate.lastmsgPerUser else MCChatCustom.lastmsgCustom
        channelData.collectFirst({ case data if data.channel.getId.asLong == channel.getId.asLong => data })
            .foreach(data => data.message = null)
        //If the above didn't find anything, it's sending a message to a non-chat channel
    }

    def addStaticExcludedPlugin(event: Class[_ <: Event], plugin: String): util.HashSet[String] =
        staticExcludedPlugins.compute(event, (_, hs: util.HashSet[String]) =>
            if (hs == null) Sets.newHashSet(plugin) else if (hs.add(plugin)) hs else hs)

    def callEventExcludingSome(event: Event): Unit = {
        if (notEnabled) return ()
        val second = staticExcludedPlugins.get(event.getClass)
        val first = module.excludedPlugins.get
        val both = if (second.isEmpty) first
        else util.Arrays.copyOf(first, first.length + second.size)
        var i = first.length
        if (second.nonEmpty) {
            for (plugin <- second.get.asScala) {
                both(i) = plugin
                i += 1
            }
        }
        callEventExcluding(event, false, both: _*)
    }

    /**
     * Calls an event with the given details.
     * <p>
     * This method only synchronizes when the event is not asynchronous.
     *
     * @param event   Event details
     * @param only    Flips the operation and <b>includes</b> the listed plugins
     * @param plugins The plugins to exclude. Not case sensitive.
     */
    def callEventExcluding(event: Event, only: Boolean, plugins: String*): Unit = { // Copied from Spigot-API and modified a bit
        if (event.isAsynchronous) {
            if (Thread.holdsLock(Bukkit.getPluginManager)) throw new IllegalStateException(event.getEventName + " cannot be triggered asynchronously from inside synchronized code.")
            if (Bukkit.getServer.isPrimaryThread) throw new IllegalStateException(event.getEventName + " cannot be triggered asynchronously from primary server thread.")
            fireEventExcluding(event, only, plugins: _*)
        }
        else Bukkit.getPluginManager synchronized fireEventExcluding(event, only, plugins: _*)
    }

    private def fireEventExcluding(event: Event, only: Boolean, plugins: String*): Unit = {
        val handlers = event.getHandlers // Code taken from SimplePluginManager in Spigot-API
        val listeners = handlers.getRegisteredListeners
        val server = Bukkit.getServer
        for (registration <- listeners) { // Modified to exclude plugins
            if (registration.getPlugin.isEnabled
                && !plugins.exists(only ^ _.equalsIgnoreCase(registration.getPlugin.getName))) try registration.callEvent(event)
            catch {
                case ex: AuthorNagException =>
                    val plugin = registration.getPlugin
                    if (plugin.isNaggable) {
                        plugin.setNaggable(false)
                        server.getLogger.log(Level.SEVERE, String.format("Nag author(s): '%s' of '%s' about the following: %s", plugin.getDescription.getAuthors, plugin.getDescription.getFullName, ex.getMessage))
                    }
                case ex: Throwable =>
                    server.getLogger.log(Level.SEVERE, "Could not pass event " + event.getEventName + " to " + registration.getPlugin.getDescription.getFullName, ex)
            }
        }
    }

    /**
     * Call it from an async thread.
     */
    def callLoginEvents(dcp: DiscordConnectedPlayer): Unit = {
        val loginFail = (kickMsg: String) => {
            dcp.sendMessage("Minecraft chat disabled, as the login failed: " + kickMsg)
            MCChatPrivate.privateMCChat(dcp.getChannel, start = false, dcp.getUser, dcp.getChromaUser)
        } //Probably also happens if the user is banned or so
        val event = new AsyncPlayerPreLoginEvent(dcp.getName, InetAddress.getLoopbackAddress, dcp.getUniqueId)
        callEventExcludingSome(event)
        if (event.getLoginResult ne AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loginFail(event.getKickMessage)
            return ()
        }
        Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () => {
            def foo(): Unit = {
                val ev = new PlayerLoginEvent(dcp, "localhost", InetAddress.getLoopbackAddress)
                callEventExcludingSome(ev)
                if (ev.getResult ne PlayerLoginEvent.Result.ALLOWED) {
                    loginFail(ev.getKickMessage)
                    return ()
                }
                callEventExcludingSome(new PlayerJoinEvent(dcp, ""))
                dcp.setLoggedIn(true)
                if (module != null) {
                    // TODO: ServerWatcher
                    module.log(dcp.getName + " (" + dcp.getUniqueId + ") logged in from Discord")
                }
            }

            foo()
        })
    }

    /**
     * Only calls the events if the player is actually logged in
     *
     * @param dcp       The player
     * @param needsSync Whether we're in an async thread
     */
    def callLogoutEvent(dcp: DiscordConnectedPlayer, needsSync: Boolean): Unit = {
        if (!dcp.isLoggedIn) return ()
        val event = new PlayerQuitEvent(dcp, "")
        if (needsSync) callEventSync(event)
        else callEventExcludingSome(event)
        dcp.setLoggedIn(false)
        if (module != null) {
            module.log(dcp.getName + " (" + dcp.getUniqueId + ") logged out from Discord")
            // TODO: ServerWatcher
        }
    }

    private[mcchat] def callEventSync(event: Event) = Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () => callEventExcludingSome(event))

    class LastMsgData(val channel: MessageChannel, val user: User) {
        var message: Message = null
        var time = 0L
        var content: String = null
        var mcchannel: component.channel.Channel = null

        protected def this(channel: MessageChannel, user: User, mcchannel: component.channel.Channel) = {
            this(channel, user)
            this.mcchannel = mcchannel
        }
    }

}