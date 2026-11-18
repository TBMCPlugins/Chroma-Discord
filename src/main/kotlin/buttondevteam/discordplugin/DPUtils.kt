package buttondevteam.discordplugin

import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import buttondevteam.lib.architecture.IHaveConfig
import buttondevteam.lib.architecture.config.IConfigData
import com.github.shynixn.mccoroutine.bukkit.launch
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.Event
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateMono
import discord4j.discordjson.json.MessageReferenceData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import java.util.logging.Logger
import java.util.regex.Pattern

object DPUtils {
    private val URL_PATTERN = Pattern.compile("https?://\\S*")
    private val FORMAT_PATTERN = Pattern.compile("[*_~]")

    fun embedWithHead(ecs: EmbedCreateSpec.Builder, displayname: String, playername: String, profileUrl: String?): EmbedCreateSpec.Builder =
        ecs.author(displayname, profileUrl, "https://minotar.net/avatar/$playername/32.png")

    /**
     * Removes §[Char] colour codes from strings & escapes them for Discord <br>
     * Ensure that this method only gets called once (escaping)
     */
    fun sanitizeString(string: String): String = escape(sanitizeStringNoEscape(string))

    /**
     * Removes §[Char] colour codes from strings
     */
    fun sanitizeStringNoEscape(string: String): String {
        val sanitizedString = StringBuilder()
        var random = false
        var i = 0
        while (i < string.length) {
            if (string[i] == '§') {
                i += 1 // Skips the data value, the 4 in "§4Alisolarflare"
                random = string[i] == 'k'
            }
            else if (!random) { // Skip random/obfuscated characters
                sanitizedString.append(string[i])
            }
            i += 1
        }
        return sanitizedString.toString()
    }

    private fun escape(message: String): String {
        val ts = TreeSet<Array<Int>>(Comparator.comparingInt({ a -> a[0] })) //Compare the start, then check the end
        var matcher = URL_PATTERN.matcher(message)
        while (matcher.find()) ts.add(arrayOf(matcher.start(), matcher.end()))
        matcher = FORMAT_PATTERN.matcher(message)
        val sb = StringBuffer()
        //Find a URL start <= our start
        //Check if URL end < our start
        while (matcher.find()) matcher.appendReplacement(sb, if ((ts.floor(arrayOf(matcher.start(), 0))?.get(1) ?: -1) < matcher.start()) ("\\\\" + matcher.group()) else matcher.group())
        matcher.appendTail(sb)
        return sb.toString()
    }

    val logger: Logger
        get() = DiscordPlugin.plugin?.logger ?: Logger.getLogger("DiscordPlugin")

    fun IHaveConfig.channelData(key: String): DiscordChannelData =
        DiscordChannelData(snowflakeData(this, key, 0L))

    fun IHaveConfig.roleData(key: String, guild: Guild = DiscordPlugin.plugin!!.mainServer!!): DiscordRoleData =
        roleData(key) { guild }

    fun IHaveConfig.roleData(key: String, guildGetter: suspend () -> Guild?): DiscordRoleData =
        DiscordRoleData(snowflakeData(this, key, 0L), guildGetter)

    fun snowflakeData(config: IHaveConfig, key: String, defID: Long) =
        config.getData(key, { id -> Snowflake.of(id as Long) }, { it.asLong() }, defID, true)

    /**
     * Mentions the <b>bot channel</b>. Useful for help texts.
     *
     * @return The string for mentioning the channel
     */
    fun botmention(): String {
        if (DiscordPlugin.plugin == null) return "#bot"
        return channelMention(DiscordPlugin.plugin!!.commandChannel.id)
    }

    suspend fun disableIfConfigErrorAny(component: Component<DiscordPlugin>?, vararg configs: DiscordEntity<*>): Boolean {
        var result = false
        for (config in configs) {
            // Print all warnings even if we already disabled the component
            result = result || disableIfConfigError(component, config)
        }
        return result
    }

    /**
     * Disables the component if one of the given configs return null. Useful for channel/role configs.
     *
     * @param component The component to disable if needed
     * @param config    The (snowflake) config to check for null
     * @return Whether the component got disabled and a warning logged
     */
    suspend fun disableIfConfigError(component: Component<DiscordPlugin>?, config: DiscordEntity<*>): Boolean {
        val result = config.getEntity()
        if (result == null) {
            val path = config.data.path
            try {
                if (component != null) Component.setComponentEnabled(component, false)
            } catch (e: Exception) {
                    if (component != null) TBMCCoreAPI.SendException("Failed to disable component after config error!", e, component)
                    else TBMCCoreAPI.SendException("Failed to disable component after config error!", e, DiscordPlugin.plugin!!)
            }
            logger.warning("The config value $path isn't set correctly " + (if (component == null) "in global settings!" else "for component " + component::class.simpleName + "!"))
            logger.warning("Set the correct ID in the config" + (if (component == null) "" else " or disable this component") + " to remove this message.")
            return true
        }
        return false
    }

    /**
     * Send a response to the message.
     *
     * @param original The original message to reply to
     * @param message  The message to send
     * @return A mono to send the message
     */
    suspend fun reply(original: Message, message: String): MessageCreateMono {
        return original.channel.awaitFirst().createMessage(message).withMessageReference(MessageReferenceData.builder().messageId(original.id.asString()).build())
    }

    fun nickMention(userId: Snowflake): String = "<@!" + userId.asString() + ">"

    fun channelMention(channelId: Snowflake): String = "<#" + channelId.asString() + ">"

    /**
     * Gets a message channel for a config. Returns empty for ID 0.
     *
     * @param key The config key
     * @param id  The channel ID
     * @return A message channel
     */
    suspend fun getMessageChannel(key: String, id: Snowflake): MessageChannel? {
        if (id.asLong() == 0L) return null

        try {
            val channel = DiscordPlugin.dc.getChannelById(id).awaitFirstOrNull()
            return channel as? MessageChannel
        } catch (e: Exception) {
            if (e is CancellationException) {
                currentCoroutineContext().ensureActive()
            }
            logger.warning("Failed to get channel data for $key=$id - ${e.message}")
            return null
        }
    }

    suspend fun getMessageChannel(config: IConfigData<Snowflake>): MessageChannel? =
        getMessageChannel(config.path, config.get())

    fun <T : Any> ignoreError(mono: Mono<T>): Mono<T> = mono.onErrorResume { Mono.empty() }

    fun net.kyori.adventure.text.Component.asString(): String {
        return PlainTextComponentSerializer.plainText().serialize(this)
    }

    fun <T : Event> Flux<T>.addHandler(handler: FlowCollector<T>) {
        DiscordPlugin.plugin!!.launch { this@addHandler.asFlow().collect(handler) }
    }
}