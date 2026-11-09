package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.ChannelconBroadcast.ChannelconBroadcast
import buttondevteam.discordplugin._
import buttondevteam.discordplugin.mcchat.MCChatCustom.CustomLMD
import buttondevteam.lib.player.ChromaGamerBase
import com.google.common.collect.Sets
import discord4j.common.util.Snowflake
import io.netty.util.collection.LongObjectHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.plugin.AuthorNagException
import reactor.core.scala.publisher.SMono

import java.net.InetAddress
import java.util
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.logging.Level
import javax.annotation.Nullable

object MCChatUtils {
    /**
     * May contain P&lt;DiscordID&gt; as key for public chat
     */
    val UnconnectedSenders = asScala(ConcurrentHashMap[String, ConcurrentHashMap[Snowflake, DiscordSender]])
    val ConnectedSenders = asScala(ConcurrentHashMap[String, ConcurrentHashMap[Snowflake, DiscordConnectedPlayer]])
    val OnlineSenders = asScala(ConcurrentHashMap[String, ConcurrentHashMap[Snowflake, DiscordPlayerSender]])
    val LoggedInPlayers = asScala(ConcurrentHashMap[UUID, DiscordConnectedPlayer])
    @Nullable private[mcchat] var lastmsgdata: MCChatUtils.LastMsgData = null
    private[mcchat] val lastmsgfromd = LongObjectHashMap[Message] // Last message sent by a Discord user, used for clearing checkmarks
    private var module: MinecraftChatModule = null
    private val staticExcludedPlugins: concurrent.Map[Class[_ <: Event], util.HashSet[String]] = concurrent.TrieMap()

    val broadcastedMessages: mutable.Map[String, Long] = mutable.Map()

    fun updatePlayerList(): Unit = {
        val mod = getModule
        if (mod == null || !mod.showPlayerListOnDC.get) return Unit
        if (lastmsgdata != null) updatePL(lastmsgdata)
        MCChatCustom.lastmsgCustom.foreach(MCChatUtils.updatePL)
    }

    private fun notEnabled = (module == null || !module.disabling) && getModule == null //Allow using things while disabling the module

    private fun getModule = {
        if (module == null || !module.isEnabled) module = ComponentManager.getIfEnabled(classOf[MinecraftChatModule])
        //If disabled, it will try to get it again because another instance may be enabled - useful for /discord restart
        module
    }

    private fun updatePL(lmd: MCChatUtils.LastMsgData): Unit = {
        if (!lmd.channel.isInstanceOf[TextChannel]) {
            TBMCCoreAPI.SendException("Failed to update player list for channel " + lmd.channel.getId, Exception("The channel isn't a (guild) text channel."), getModule)
            return Unit
        }
        var topic = lmd.channel.asInstanceOf[TextChannel].getTopic.orElse("")
        if (topic.isEmpty) topic = ".\n----\nMinecraft chat\n----\n."
        val s = topic.split("\\n----\\n")
        if (s.length < 3) return Unit
        var gid: String = null
        lmd match {
            case clmd: CustomLMD -> gid = clmd.groupID
            case _ -> //If we're not using a custom chat then it's either can ("everyone") or can't (null) see at most
                gid = buttondevteam.core.component.channel.Channel.GROUP_EVERYONE // (Though it's a public chat then rn)
        }
        val C = AtomicInteger
        s(s.length - 1) = "Players: " + Bukkit.getOnlinePlayers.asScala.filter(p -> if (lmd.mcchannel == null) {
            gid == buttondevteam.core.component.channel.Channel.GROUP_EVERYONE //If null, allow if public (custom chats will have their channel stored anyway)
        }
        else {
            gid == lmd.mcchannel.getGroupID(ChromaGamerBase.getFromSender(p))
        }
        ).filter(MCChatUtils.checkEssentials) //If they can see it
            .filter(_ -> C.incrementAndGet > 0) //Always true
            .map((p) -> DPUtils.sanitizeString(p.getDisplayName)).mkString(", ")
        s(0) = "$C player${if (C.get != 1) "" else ""} online"
        lmd.channel.asInstanceOf[TextChannel].edit().withTopic(String.join("\n----\n", s: _*)).withReason("Player list update").subscribe() //Don't wait
    }

    private[mcchat] fun checkEssentials(p: Player): Boolean = {
        val ess = MainPlugin.ess
        if (ess == null) return true
        !ess.getUser(p).isHidden
    }

    fun addSenderTo[T <(): DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], user: User, sender: T): T =
        addSenderTo(senders, user.getId.asString, sender)

    fun addSenderTo[T <(): DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], did: String, sender: T): T = {
        val mapOpt = senders.get(did)
        val map = if (mapOpt.isEmpty) ConcurrentHashMap[Snowflake, T] else mapOpt.get
        map.put(sender.getChannel.getId, sender)
        senders.put(did, map)
        sender
    }

    fun getSenderFrom[T <(): DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], channel: Snowflake, user: User): T = {
        val mapOpt = senders.get(user.getId.asString)
        if (mapOpt.nonEmpty) mapOpt.get.get(channel)
        else null.asInstanceOf
    }

    fun removeSenderFrom[T <(): DiscordSenderBase](senders: concurrent.Map[String, ConcurrentHashMap[Snowflake, T]], channel: Snowflake, user: User): T = {
        val mapOpt = senders.get(user.getId.asString)
        if (mapOpt.nonEmpty) mapOpt.get.remove(channel)
        else null.asInstanceOf
    }

    fun forPublicPrivateChat(action: SMono[MessageChannel] -> SMono[_]): SMono[_] = {
        if (notEnabled) return SMono.empty
        val list = MCChatPrivate.lastmsgPerUser.map(data -> action(SMono.just(data.channel)))
            .prepend(action(module.chatChannelMono))
        SMono.whenDelayError(list)
    }

    /**
     * For custom and all MC chat
     *
     * @param action  The action to act (cannot complete empty)
     * @param toggle  The toggle to check
     * @param hookmsg Whether the message is also sent from the hook
     */
    fun forCustomAndAllMCChat(action: SMono[MessageChannel] -> SMono[_], @Nullable toggle: ChannelconBroadcast, hookmsg: Boolean): SMono[_] = {
        if (notEnabled) return SMono.empty
        val list = List(forPublicPrivateChat(action) ) ++
            (if (toggle == null) MCChatCustom.lastmsgCustom
            else MCChatCustom.lastmsgCustom.filter(cc -> (cc.toggles & (1 << toggle.id)) != 0))
                .map(_.channel).map(SMono.just).map(action)
        SMono.whenDelayError(list)
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action The action to do
     * @param permUser The user to check perms of or null to send to all that has it toggled
     * @param toggle The toggle to check or null to send to all allowed
     */
    fun forAllowedCustomMCChat(action: SMono[MessageChannel] -> SMono[_], @Nullable permUser: ChromaGamerBase, @Nullable toggle: ChannelconBroadcast): SMono[_] = {
        if (notEnabled) return SMono.empty
        val st = MCChatCustom.lastmsgCustom.filter(clmd -> { //TBMCChannelConnectFakeEvent(sender, clmd.mcchannel).shouldSendTo(clmd.dcp) - Thought it was this simple hehe - Wait, it *should* be this simple
            if (toggle != null && ((clmd.toggles & (1 << toggle.id)) == 0)) false //If null then allow
            else if (permUser == null) true
            else clmd.groupID.equals(clmd.mcchannel.getGroupID(permUser))
        }).map(cc -> action.apply(SMono.just(cc.channel))) //TODO: Send error messages on channel connect
        //Mono.whenDelayError((() -> st.iterator).asInstanceOf[java.lang.Iterable[SMono[_]]]) //Can't convert as an iterator or inside the stream, but I can convert it as a stream
        SMono.whenDelayError(st)
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action  The action to do
     * @param permUser  The user to check perms of or null to send to all that has it toggled
     * @param toggle  The toggle to check or null to send to all allowed
     * @param hookmsg Whether the message is also sent from the hook
     */
    fun forAllowedCustomAndAllMCChat(action: SMono[MessageChannel] -> SMono[_], @Nullable permUser: ChromaGamerBase, @Nullable toggle: ChannelconBroadcast, hookmsg: Boolean): SMono[_] = {
        if (notEnabled) return SMono.empty
        val cc = forAllowedCustomMCChat(action, permUser, toggle)
        return SMono.whenDelayError(List(forPublicPrivateChat(action), cc))
    }

    fun send(message: String): SMono[MessageChannel] -> SMono[_] = _.flatMap((mc: MessageChannel) -> {
        resetLastMessage(mc)
        mc.createMessage(DPUtils.sanitizeString(message)).^^()
    })

    fun forAllowedMCChat(action: SMono[MessageChannel] -> SMono[_], event: TBMCSystemChatEvent): SMono[_] = {
        if (notEnabled) return SMono.empty
        val list = ListBuffer[SMono[_]]
        if (event.getChannel.isGlobal) list.append(action(module.chatChannelMono))
        for (data <- MCChatPrivate.lastmsgPerUser)
            if (event.shouldSendTo(data.user))
                list.append(action(SMono.just(data.channel))) //TODO: Only store ID?
        MCChatCustom.lastmsgCustom.filter(clmd ->
            clmd.brtoggles.contains(event.getTarget) && event.shouldSendTo(clmd.dcp.getChromaUser))
            .map(clmd -> action(SMono.just(clmd.channel))).foreach(elem -> {
            list.append(elem)
            ()
        })
        SMono.whenDelayError(list)
    }

    /**
     * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
     */
    private[mcchat] fun getSender(channel: Snowflake, author: User): DiscordSenderBase = {
        Option(getSenderFrom(OnlineSenders, channel, author)) // Find first non-null
            .orElse(Option(getSenderFrom(ConnectedSenders, channel, author))) // This doesn't support the public chat, but it'll always return null for it
            .orElse(Option(getSenderFrom(UnconnectedSenders, channel, author))) //
            .orElse(Option(addSenderTo(UnconnectedSenders, author,
                DiscordSender(author, DiscordPlugin.dc.getChannelById(channel).block().asInstanceOf[MessageChannel]))))
            .get
    }

    /**
     * Resets the last message, so it will start a new one instead of appending to it.
     * This is used when someone (even the bot) sends a message to the channel.
     *
     * @param channel The channel to reset in - the process is slightly different for the public, private and custom chats
     */
    fun resetLastMessage(channel: Channel): Unit = {
        if (notEnabled) return Unit
        if (channel.getId.asLong == module.chatChannel.get.asLong) {
            if (lastmsgdata == null) lastmsgdata = MCChatUtils.LastMsgData(module.chatChannelMono.block(), null)
            else lastmsgdata.message = null
            return Unit
        } // Don't set the whole object to null, the player and channel information should be preserved
        val channelData = if (channel.isInstanceOf[PrivateChannel]) MCChatPrivate.lastmsgPerUser else MCChatCustom.lastmsgCustom
        channelData.collectFirst({ case data if data.channel.getId.asLong == channel.getId.asLong -> data })
            .foreach(data -> data.message = null)
        //If the above didn't find anything, it's sending a message to a non-chat channel
    }

    fun addStaticExcludedPlugin(event: Class[_ <: Event], plugin: String): util.HashSet[String] =
        staticExcludedPlugins.compute(event, (_, hs: util.HashSet[String]) ->
            if (hs == null) Sets.newHashSet(plugin) else if (hs.add(plugin)) hs else hs)

    fun callEventExcludingSome(event: Event): Unit = {
        if (notEnabled) return Unit
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
    fun callEventExcluding(event: Event, only: Boolean, plugins: String*): Unit = { // Copied from Spigot-API and modified a bit
        if (event.isAsynchronous) {
            if (Thread.holdsLock(Bukkit.getPluginManager)) throw IllegalStateException(event.getEventName + " cannot be triggered asynchronously from inside synchronized code.")
            if (Bukkit.getServer.isPrimaryThread) throw IllegalStateException(event.getEventName + " cannot be triggered asynchronously from primary server thread.")
            fireEventExcluding(event, only, plugins: _*)
        }
        else Bukkit.getPluginManager synchronized fireEventExcluding(event, only, plugins: _*)
    }

    private fun fireEventExcluding(event: Event, only: Boolean, plugins: String*): Unit = {
        val handlers = event.getHandlers // Code taken from SimplePluginManager in Spigot-API
        val listeners = handlers.getRegisteredListeners
        val server = Bukkit.getServer
        for (registration <- listeners) { // Modified to exclude plugins
            if (registration.getPlugin.isEnabled
                && !plugins.exists(only ^ _.equalsIgnoreCase(registration.getPlugin.getName))) try registration.callEvent(event)
            catch {
                case ex: AuthorNagException ->
                    val plugin = registration.getPlugin
                    if (plugin.isNaggable) {
                        plugin.setNaggable(false)
                        server.getLogger.log(Level.SEVERE, String.format("Nag author(s): '%s' of '%s' about the following: %", plugin.getDescription.getAuthors, plugin.getDescription.getFullName, ex.getMessage))
                    }
                case ex: Throwable ->
                    server.getLogger.log(Level.SEVERE, "Could not pass event " + event.getEventName + " to " + registration.getPlugin.getDescription.getFullName, ex)
            }
        }
    }

    /**
     * Call it from an async thread.
     */
    def callLoginEvents(dcp: DiscordConnectedPlayer): Unit = {
        val loginFail = (kickMsg: String) -> {
            dcp.sendMessage("Minecraft chat disabled, as the login failed: " + kickMsg)
            MCChatPrivate.privateMCChat(dcp.getChannel, start = false, dcp.getUser, dcp.getChromaUser)
        } //Probably also happens if the user is banned or so
        val event = AsyncPlayerPreLoginEvent(dcp.getName, InetAddress.getLoopbackAddress, dcp.getUniqueId)
        callEventExcludingSome(event)
        if (event.getLoginResult ne AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            loginFail(event.getKickMessage)
            return Unit
        }
        Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () -> {
            def foo(): Unit = {
                val ev = PlayerLoginEvent(dcp, "localhost", InetAddress.getLoopbackAddress)
                callEventExcludingSome(ev)
                if (ev.getResult ne PlayerLoginEvent.Result.ALLOWED) {
                    loginFail(ev.getKickMessage)
                    return Unit
                }
                callEventExcludingSome(PlayerJoinEvent(dcp, ""))
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
        if (!dcp.isLoggedIn) return Unit
        val event = PlayerQuitEvent(dcp, "")
        if (needsSync) callEventSync(event)
        else callEventExcludingSome(event)
        dcp.setLoggedIn(false)
        if (module != null) {
            module.log(dcp.getName + " (" + dcp.getUniqueId + ") logged out from Discord")
            // TODO: ServerWatcher
        }
    }

    private[mcchat] fun callEventSync(event: Event) = Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () -> callEventExcludingSome(event))

    class LastMsgData(val channel: MessageChannel, @Nullable val user: ChromaGamerBase) {

        var message: Message = null
        var time = 0L
        var content: String = null
        var mcchannel: component.channel.Channel = null

        protected fun this(channel: MessageChannel, mcchannel: component.channel.Channel, user: ChromaGamerBase) = {
            this(channel, user)
            this.mcchannel = mcchannel
        }
    }

}