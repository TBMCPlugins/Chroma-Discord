package buttondevteam.discordplugin.`fun`

import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DPUtils.channelData
import buttondevteam.discordplugin.DPUtils.roleData
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.GuildChannel
import discord4j.core.`object`.presence.Status
import discord4j.core.spec.EmbedCreateSpec
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.any
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*
import java.util.Locale.getDefault
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random

/**
 * All kinds of random things.
 * The YEEHAW event uses an emoji named :YEEHAW: if available
 */
class FunModule : Component<DiscordPlugin>(), Listener {
    private val serverReadyStrings = arrayOf(
        "in one week from now", // Ali
        "between now and the heat-death of the universe.", // Ghostise
        "soon™", "ask again this time next month", "in about 3 seconds", // Nicolai
        "after we finish 8 plugins", "tomorrow.", "after one tiiiny feature",
        "next commit", "after we finish strangling Towny", "when we kill every *fucking* bug",
        "once the server stops screaming.", "after HL3 comes out", "next time you ask",
        "when will *you* be open?" // Ali
    )
    private val usableServerReadyStrings = arrayListOf<Short>()
    private var lastlist = 0
    private var lastlistp = 0
    private var ListC = 0

    suspend fun executeMemes(message: Message): Boolean {
        val msglowercased = message.content.lowercase(getDefault())
        lastlist += 1
        if (lastlist > 5) {
            ListC = 0
            lastlist = 0
        }
        if (msglowercased == "/list" && Bukkit.getOnlinePlayers().size == lastlistp && ListC++ > 2) { // Lowered already
            DPUtils.reply(message, "stop it. You know the answer.").awaitFirstOrNull()
            lastlist = 0
            lastlistp = Bukkit.getOnlinePlayers().size
            return true //Handled
        }
        lastlistp = Bukkit.getOnlinePlayers().size //Didn't handle
        if (!TBMCCoreAPI.IsTestServer() && this.serverReady.any { msglowercased.contains(it) }) {
            if (usableServerReadyStrings.isEmpty()) this.createUsableServerReadyStrings()
            val next = usableServerReadyStrings.removeAt(Random.nextInt(usableServerReadyStrings.size))
            DPUtils.reply(message, this.serverReadyAnswers[next.toInt()]).awaitFirstOrNull()
            return false //Still process it as a command/mcchat if needed
        }
        return false
    }

    private var lasttime: Long = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun handleFullHouse(event: PresenceUpdateEvent) {
        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 5 != 0) return
        if (event.old.getOrNull()?.status != Status.OFFLINE || event.current.status == Status.OFFLINE)
            return //If it's not an offline -> online change
        val channel = fullHouseChannel.getEntity() ?: return // Stop early if the channel is not set
        val guild = event.guild.awaitFirstOrNull() ?: return
        val member = event.member.awaitFirstOrNull() ?: return
            if (!member.roles.asFlow().any { it.id.asLong() == fullHouseDevRole.id.asLong() }) return
        val fullHouseMembers = guild.members.asFlow().filter { it.roleIds.contains(fullHouseDevRole.id) }
        if (fullHouseMembers.flatMapConcat { it.presence.asFlow() }.any { it.status == Status.OFFLINE }) return
        if (lasttime + 10 > TimeUnit.NANOSECONDS.toHours(System.nanoTime())) return //This should stay so it checks this last

        lasttime = TimeUnit.NANOSECONDS.toHours(System.nanoTime())
        val imageUrl = "https://cdn.discordapp.com/attachments/249295547263877121/249687682618359808/poker-hand-full-house-aces-kings-playing-cards-15553791.png"
        channel.createMessage("Full house!").withEmbeds(EmbedCreateSpec.builder().image(imageUrl).build()).awaitFirstOrNull()
    }

    /**
     * Questions that the bot will choose a random answer to give to.
     */
    private val serverReady by
        config.getListData("serverReady", listOf(
            "when will the server be open", "when will the server be ready",
            "when will the server be done", "when will the server be complete",
            "when will the server be finished", "when's the server ready",
            "when's the server open", "vhen vill ze server be open?"
        ))

    /**
     * Answers for a recognized question. Selected randomly.
     */
    private val serverReadyAnswers by config.getData("serverReadyAnswers", serverReadyStrings)

    private fun createUsableServerReadyStrings() =
        IntStream.range(0, serverReadyAnswers.size).forEach { i -> usableServerReadyStrings.add(i.toShort()) }

    override fun enable() {
        registerListener(this) // TODO: Suspending listener
    }

    override fun disable() {
        lastlist = 0
        lastlistp = 0
        ListC = 0
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        ListC = 0
    }

    /**
     * The channel to post the full house to.
     */
    private val fullHouseChannel = config.channelData("fullHouseChannel")

    /**
     * If all of the people who have this role are online, the bot will post a full house.
     */
    private val fullHouseDevRole = config.roleData("fullHouseDevRole", suspend {
        val channel = fullHouseChannel.getEntity()
        if (channel is GuildChannel) channel.guild.awaitFirstOrNull()
        else null
    })
}