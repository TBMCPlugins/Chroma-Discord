package buttondevteam.discordplugin

import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.Component
import buttondevteam.lib.architecture.IHaveConfig
import buttondevteam.lib.architecture.config.IConfigData
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.core.spec.MessageCreateMono
import discord4j.core.spec.legacy.LegacySpec
import discord4j.discordjson.json.MessageReferenceData
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import reactor.core.publisher.Mono

import java.util.Comparator
import java.util.TreeSet
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.annotation.Nullable

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

    // TODO: Create a proxy that supports suspend functions, based on Snowflake data
    fun channelData(config: IHaveConfig, key: String): IConfigData<Mono<MessageChannel>> =
        config.getData(key, { id -> getMessageChannel(key, Snowflake.of(id as Long)) },
            { 0L }, 0L, true) //We can afford to search for the channel in the cache once (instead of using mainServer)

    fun roleData(config: IHaveConfig, key: String, defName: String): IConfigData<Mono<Role>> =
        roleData(config, key, defName, Mono.just(DiscordPlugin.plugin!!.mainServer!!))

    /**
     * Needs to be a [buttondevteam.lib.architecture.ConfigData] for checking if it's set
     */
    fun roleData(config: IHaveConfig, key: String, defName: String, guild: Mono<Guild>): IConfigData<Mono<Role>> = config.getData(key, { name ->
        if (name !is String || name.isEmpty()) Mono.empty<Role>()
        else guild.flatMapMany({ it.roles }).filter({ it.name == name }).onErrorResume({ e ->
            logger.warning("Failed to get role data for " + key + "=" + name + " - " + e.message)
            Mono.empty()
        }).next()
    }, { _ -> defName }, defName, true)

    fun snowflakeData(config: IHaveConfig, key: String, defID: Long) =
        config.getData(key, { id -> Snowflake.of(id as Long) }, { it.asLong() }, defID, true)

    /**
     * Mentions the <b>bot channel</b>. Useful for help texts.
     *
     * @return The string for mentioning the channel
     */
    fun botmention(): String {
        if (DiscordPlugin.plugin == null) return "#bot"
        return channelMention(DiscordPlugin.plugin!!.commandChannel)
    }

    /**
     * Disables the component if one of the given configs return null. Useful for channel/role configs.
     *
     * @param component The component to disable if needed
     * @param configs   The configs to check for null
     * @return Whether the component got disabled and a warning logged
     */
    fun disableIfConfigError(component: Component<DiscordPlugin>?, vararg configs: IConfigData<*>): Boolean {
        // TODO: Tie the DP configs to the plugin/component and automate this (disable on error/null)
        for (config in configs) {
            val v = config.get()
            if (disableIfConfigErrorRes(component, config, v)) return true
        }
        return false
    }

    /**
     * Disables the component if one of the given configs return null. Useful for channel/role configs.
     *
     * @param component The component to disable if needed
     * @param config    The (snowflake) config to check for null
     * @param result    The result of getting the value
     * @return Whether the component got disabled and a warning logged
     */
    fun disableIfConfigErrorRes(component: Component<DiscordPlugin>?, config: IConfigData<*>, result: Any?): Boolean {
        //noinspection ConstantConditions
        if (result == null) {
            var path: String? = try {
                if (component != null) Component.setComponentEnabled(component, false)
                config.path
            } catch (e: Exception) {
                    if (component != null) TBMCCoreAPI.SendException("Failed to disable component after config error!", e, component)
                    else TBMCCoreAPI.SendException("Failed to disable component after config error!", e, DiscordPlugin.plugin!!)
                null
            }
            logger.warning(
                "The config value $path isn't set correctly " + (
                    if (component == null) "in global settings!"
            else "for component " + component::class.simpleName + "!"))
            logger.warning("Set the correct ID in the config" + (if (component == null) ""
            else " or disable this component") + " to remove this message.")
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
        return original.channel.asFlow().first().createMessage(message).withMessageReference(MessageReferenceData.builder().messageId(original.id.asString()).build())
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
            val channel = DiscordPlugin.dc.getChannelById(id).asFlow().firstOrNull()
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
}