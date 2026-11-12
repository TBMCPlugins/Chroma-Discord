package buttondevteam.discordplugin.mcchat

import buttondevteam.core.ComponentManager
import buttondevteam.core.MainPlugin
import buttondevteam.core.component.channel.Channel
import buttondevteam.discordplugin.ChannelconBroadcast
import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordSender
import buttondevteam.discordplugin.mcchat.sender.DiscordSenderBase
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.TBMCSystemChatEvent
import buttondevteam.lib.player.ChromaGamerBase
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.channel.TextChannel
import io.netty.util.collection.LongObjectHashMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import reactor.core.publisher.Mono
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

object MCChatUtils {
    /**
     * May contain P&lt;DiscordID&gt; as key for public chat
     */
    val UnconnectedSenders = ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, DiscordSender>>()
    internal var lastmsgdata: LastMsgData? = null
    internal val lastmsgfromd =
        LongObjectHashMap<Message>() // Last message sent by a Discord user, used for clearing checkmarks
    private var module: MinecraftChatModule? = null

    val broadcastedMessages = HashMap<String, Long>()

    fun updatePlayerList(): Unit {
        val mod = getModule()
        if (mod == null || !mod.showPlayerListOnDC) return Unit
        lastmsgdata?.let { updatePL(it) }
    }

    private fun notEnabled() =
        (!(module?.disabling ?: false)) && getModule() == null //Allow using things while disabling the module

    private fun getModule(): MinecraftChatModule? {
        val m = module
        if (m == null || !m.isEnabled) {
            module = ComponentManager.getIfEnabled(MinecraftChatModule::class.java)
            return module
        }
        //If disabled, it will try to get it again because another instance may be enabled - useful for /discord restart
        return m
    }

    private fun updatePL(lmd: LastMsgData) {
        if (lmd.channel !is TextChannel) {
            TBMCCoreAPI.SendException(
                "Failed to update player list for channel " + lmd.channel.id,
                Exception("The channel isn't a (guild) text channel."),
                getModule()!!
            )
            return
        }
        var topic = lmd.channel.topic.orElse("")
        if (topic.isEmpty()) topic = ".\n----\nMinecraft chat\n----\n."
        val s = topic.split("\\n----\\n").toMutableList()
        if (s.size < 3) return
        var gid: String? = null
        gid = Channel.GROUP_EVERYONE // TODO: Custom chat channels
        val C = AtomicInteger()
        s[s.size - 1] = "Players: " + Bukkit.getOnlinePlayers().filter { p ->
            val mcchannel = lmd.mcchannel
            if (mcchannel == null) {
                gid == Channel.GROUP_EVERYONE //If null, allow if public (custom chats will have their channel stored anyway)
            } else {
                gid == mcchannel.getGroupID(ChromaGamerBase.getFromSender(p))
            }
        }.filter { checkEssentials(it) } //If they can see it
            .filter { C.incrementAndGet() > 0 }.joinToString(", ") { p -> DPUtils.sanitizeString(p.displayName) }
        s[0] = "$C player${if (C.get() != 1) "s" else ""} online"
        lmd.channel.edit().withTopic(s.joinToString("\n----\n")).withReason("Player list update")
            .subscribe() //Don't wait
    }

    internal fun checkEssentials(p: Player): Boolean {
        val ess = MainPlugin.ess
        if (ess == null) return true
        return !ess.getUser(p).isHidden
    }

    fun <T : DiscordSenderBase> addSenderTo(
        senders: ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>>,
        user: User,
        sender: T
    ): T =
        addSenderTo(senders, user.id.asString(), sender)

    fun <T : DiscordSenderBase> addSenderTo(
        senders: ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>>,
        did: String,
        sender: T
    ): T {
        val mapOpt = senders[did]
        val map = mapOpt ?: ConcurrentHashMap<Snowflake, T>()
        map[sender.getChannel().id] = sender
        senders[did] = map
        return sender
    }

    fun <T : DiscordSenderBase> getSenderFrom(
        senders: ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>>,
        channel: Snowflake,
        user: User
    ): T? {
        val mapOpt = senders[user.id.asString()]
        return mapOpt?.get(channel)
    }

    fun <T : DiscordSenderBase> removeSenderFrom(
        senders: ConcurrentHashMap<String, ConcurrentHashMap<Snowflake, T>>,
        channel: Snowflake,
        user: User
    ): T? {
        val mapOpt = senders[user.id.asString()]
        return mapOpt?.remove(channel)
    }

    fun forPublicPrivateChat(action: (ch: Mono<MessageChannel>) -> Mono<*>): Mono<*> {
        if (notEnabled()) return Mono.empty<Any>()
        val list = listOf(action(Mono.just(module!!.chatChannelMono!!)))
        // TODO: Private chat
        return Mono.whenDelayError(list)
    }

    /**
     * For custom and all MC chat
     *
     * @param action  The action to act (cannot complete empty)
     * @param toggle  The toggle to check
     * @param hookmsg Whether the message is also sent from the hook
     */
    fun forCustomAndAllMCChat(
        action: ((ch: Mono<MessageChannel>) -> Mono<*>),
        toggle: ChannelconBroadcast?,
        hookmsg: Boolean
    ): Mono<*> {
        if (notEnabled()) return Mono.empty<Any>()
        val list = listOf(forPublicPrivateChat(action))
        return Mono.whenDelayError(list)
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action The action to do
     * @param permUser The user to check perms of or null to send to all that has it toggled
     * @param toggle The toggle to check or null to send to all allowed
     */
    fun forAllowedCustomMCChat(
        action: ((ch: Mono<MessageChannel>) -> Mono<*>),
        permUser: ChromaGamerBase?,
        toggle: ChannelconBroadcast?
    ): Mono<*> {
        if (notEnabled()) return Mono.empty<Any>()
        return Mono.empty<Any>()
    }

    /**
     * Do the {@code action} for each custom chat the {@code sender} have access to and has that broadcast type enabled.
     *
     * @param action  The action to do
     * @param permUser  The user to check perms of or null to send to all that has it toggled
     * @param toggle  The toggle to check or null to send to all allowed
     * @param hookmsg Whether the message is also sent from the hook
     */
    fun forAllowedCustomAndAllMCChat(
        action: ((ch: Mono<MessageChannel>) -> Mono<*>),
        permUser: ChromaGamerBase?,
        toggle: ChannelconBroadcast?,
        hookmsg: Boolean
    ): Mono<*> {
        if (notEnabled()) return Mono.empty<Any>()
        val cc = forAllowedCustomMCChat(action, permUser, toggle)
        return Mono.whenDelayError(listOf(forPublicPrivateChat(action), cc))
    }

    fun send(message: String): ((ch: Mono<MessageChannel>) -> Mono<*>) = {
        it.flatMap { mc ->
            resetLastMessage(mc)
            mc.createMessage(DPUtils.sanitizeString(message))
        }
    }

    fun forAllowedMCChat(action: ((ch: Mono<MessageChannel>) -> Mono<*>), event: TBMCSystemChatEvent): Mono<*> {
        if (notEnabled()) return Mono.empty<Any>()
        val list = mutableListOf<Mono<*>>()
        if (event.channel.isGlobal) list += action(Mono.just(module!!.chatChannelMono!!))
        return Mono.whenDelayError(list)
    }

    /**
     * This method will find the best sender to use: if the player is online, use that, if not but connected then use that etc.
     */
    internal fun getSender(channel: Snowflake, author: User): DiscordSenderBase {
        return getSenderFrom(UnconnectedSenders, channel, author) ?: addSenderTo(
            UnconnectedSenders,
            author,
            DiscordSender(author, DiscordPlugin.dc.getChannelById(channel).block() as MessageChannel)
        )
    }

    /**
     * Resets the last message, so it will start a new one instead of appending to it.
     * This is used when someone (even the bot) sends a message to the channel.
     *
     * @param channel The channel to reset in - the process is slightly different for the public, private and custom chats
     */
    fun resetLastMessage(channel: MessageChannel) {
        if (notEnabled()) return
        if (channel.id.asLong() == module!!.chatChannel.asLong()) {
            if (lastmsgdata == null) lastmsgdata = LastMsgData(module!!.chatChannelMono!!, null)
            else lastmsgdata!!.message = null
            return
        } // Don't set the whole object to null, the player and channel information should be preserved
        // TODO: Private and custom chat
        //If the above didn't find anything, it's sending a message to a non-chat channel
    }

    open class LastMsgData(val channel: MessageChannel, val user: ChromaGamerBase?) {

        var message: Message? = null
        var time = 0L
        var content: String = ""
        var mcchannel: Channel? = null

        protected constructor(channel: MessageChannel, mcchannel: Channel, user: ChromaGamerBase) : this(
            channel,
            user
        ) {
            this.mcchannel = mcchannel
        }
    }

}