package buttondevteam.discordplugin

import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.{Component, ConfigData, IHaveConfig, ReadOnlyConfigData}
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.`object`.entity.{Guild, Message, Role}
import discord4j.core.spec.legacy.{LegacyEmbedCreateSpec, LegacySpec}
import reactor.core.publisher.{Flux, Mono}
import reactor.core.scala.publisher.{SFlux, SMono}

import java.util
import java.util.Comparator
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.annotation.Nullable

object DPUtils {
    private val URL_PATTERN = Pattern.compile("https?://\\S*")
    private val FORMAT_PATTERN = Pattern.compile("[*_~]")

    def embedWithHead(ecs: LegacyEmbedCreateSpec, displayname: String, playername: String, profileUrl: String): LegacyEmbedCreateSpec =
        ecs.setAuthor(displayname, profileUrl, "https://minotar.net/avatar/" + playername + "/32.png")

    /**
     * Removes §[char] colour codes from strings & escapes them for Discord <br>
     * Ensure that this method only gets called once (escaping)
     */
    def sanitizeString(string: String): String = escape(sanitizeStringNoEscape(string))

    /**
     * Removes §[char] colour codes from strings
     */
    def sanitizeStringNoEscape(string: String): String = {
        val sanitizedString = new StringBuilder
        var random = false
        var i = 0
        while ( {
            i < string.length
        }) {
            if (string.charAt(i) == '§') {
                i += 1 // Skips the data value, the 4 in "§4Alisolarflare"
                random = string.charAt(i) == 'k'
            }
            else if (!random) { // Skip random/obfuscated characters
                sanitizedString.append(string.charAt(i))
            }
            i += 1
        }
        sanitizedString.toString
    }

    private def escape(message: String) = { //var ts = new TreeSet<>();
        val ts = new util.TreeSet[Array[Int]](Comparator.comparingInt((a: Array[Int]) => a(0)): Comparator[Array[Int]]) //Compare the start, then check the end
        var matcher = URL_PATTERN.matcher(message)
        while ( {
            matcher.find
        }) ts.add(Array[Int](matcher.start, matcher.end))
        matcher = FORMAT_PATTERN.matcher(message)
        /*Function<MatchResult, String> aFunctionalInterface = result ->
              Optional.ofNullable(ts.floor(new int[]{result.start(), 0})).map(a -> a[1]).orElse(0) < result.start()
                ? "\\\\" + result.group() : result.group();
            return matcher.replaceAll(aFunctionalInterface); //Find nearest URL match and if it's not reaching to the char then escape*/ val sb = new StringBuffer
        while ( {
            matcher.find
        }) matcher.appendReplacement(sb, if (Option(ts.floor(Array[Int](matcher.start, 0))).map( //Find a URL start <= our start
            (a: Array[Int]) => a(1)).getOrElse(-1) < matcher.start //Check if URL end < our start
        ) "\\\\" + matcher.group else matcher.group)
        matcher.appendTail(sb)
        sb.toString
    }

    def getLogger: Logger = {
        if (DiscordPlugin.plugin == null || DiscordPlugin.plugin.getLogger == null) return Logger.getLogger("DiscordPlugin")
        DiscordPlugin.plugin.getLogger
    }

    def channelData(config: IHaveConfig, key: String): ReadOnlyConfigData[SMono[MessageChannel]] =
        config.getReadOnlyDataPrimDef(key, 0L, (id: Any) =>
            getMessageChannel(key, Snowflake.of(id.asInstanceOf[Long])), (_: SMono[MessageChannel]) => 0L) //We can afford to search for the channel in the cache once (instead of using mainServer)
    def roleData(config: IHaveConfig, key: String, defName: String): ReadOnlyConfigData[SMono[Role]] =
        roleData(config, key, defName, SMono.just(DiscordPlugin.mainServer))

    /**
     * Needs to be a [[ConfigData]] for checking if it's set
     */
    def roleData(config: IHaveConfig, key: String, defName: String, guild: SMono[Guild]): ReadOnlyConfigData[SMono[Role]] = config.getReadOnlyDataPrimDef(key, defName, (name: Any) => {
        def foo(name: Any): SMono[Role] = {
            if (!name.isInstanceOf[String] || name.asInstanceOf[String].isEmpty) return SMono.empty[Role]
            guild.flatMapMany(_.getRoles).filter((r: Role) => r.getName == name).onErrorResume((e: Throwable) => {
                def foo(e: Throwable): SMono[Role] = {
                    getLogger.warning("Failed to get role data for " + key + "=" + name + " - " + e.getMessage)
                    SMono.empty[Role]
                }

                foo(e)
            }).next
        }

        foo(name)
    }, (_: SMono[Role]) => defName)

    def snowflakeData(config: IHaveConfig, key: String, defID: Long): ReadOnlyConfigData[Snowflake] =
        config.getReadOnlyDataPrimDef(key, defID, (id: Any) => Snowflake.of(id.asInstanceOf[Long]), _.asLong)

    /**
     * Mentions the <b>bot channel</b>. Useful for help texts.
     *
     * @return The string for mentioning the channel
     */
    def botmention: String = {
        if (DiscordPlugin.plugin == null) return "#bot"
        channelMention(DiscordPlugin.plugin.commandChannel.get)
    }

    /**
     * Disables the component if one of the given configs return null. Useful for channel/role configs.
     *
     * @param component The component to disable if needed
     * @param configs   The configs to check for null
     * @return Whether the component got disabled and a warning logged
     */
    def disableIfConfigError(@Nullable component: Component[DiscordPlugin], configs: ConfigData[_]*): Boolean = {
        for (config <- configs) {
            val v = config.get
            if (disableIfConfigErrorRes(component, config, v)) return true
        }
        false
    }

    /**
     * Disables the component if one of the given configs return null. Useful for channel/role configs.
     *
     * @param component The component to disable if needed
     * @param config    The (snowflake) config to check for null
     * @param result    The result of getting the value
     * @return Whether the component got disabled and a warning logged
     */
    def disableIfConfigErrorRes(@Nullable component: Component[DiscordPlugin], config: ConfigData[_], result: Any): Boolean = {
        //noinspection ConstantConditions
        if (result == null || (result.isInstanceOf[SMono[_]] && !result.asInstanceOf[SMono[_]].hasElement.block())) {
            var path: String = null
            try {
                if (component != null) Component.setComponentEnabled(component, false)
                path = config.getPath
            } catch {
                case e: Exception =>
                    if (component != null) TBMCCoreAPI.SendException("Failed to disable component after config error!", e, component)
                    else TBMCCoreAPI.SendException("Failed to disable component after config error!", e, DiscordPlugin.plugin)
            }
            getLogger.warning("The config value " + path + " isn't set correctly " + (if (component == null) "in global settings!"
            else "for component " + component.getClass.getSimpleName + "!"))
            getLogger.warning("Set the correct ID in the config" + (if (component == null) ""
            else " or disable this component") + " to remove this message.")
            return true
        }
        false
    }

    /**
     * Send a response in the form of "@User, message". Use SMono.empty() if you don't have a channel object.
     *
     * @param original The original message to reply to
     * @param channel  The channel to send the message in, defaults to the original
     * @param message  The message to send
     * @return A mono to send the message
     */
    def reply(original: Message, @Nullable channel: MessageChannel, message: String): SMono[Message] = {
        val ch = if (channel == null) SMono(original.getChannel)
        else SMono.just(channel)
        reply(original, ch, message)
    }

    /**
     * @see #reply(Message, MessageChannel, String)
     */
    def reply(original: Message, ch: SMono[MessageChannel], message: String): SMono[Message] =
        ch.flatMap(channel => SMono(channel.createMessage((if (original.getAuthor.isPresent)
            original.getAuthor.get.getMention + ", "
        else "") + message)))

    def nickMention(userId: Snowflake): String = "<@!" + userId.asString + ">"

    def channelMention(channelId: Snowflake): String = "<#" + channelId.asString + ">"

    /**
     * Gets a message channel for a config. Returns empty for ID 0.
     *
     * @param key The config key
     * @param id  The channel ID
     * @return A message channel
     */
    def getMessageChannel(key: String, id: Snowflake): SMono[MessageChannel] = {
        if (id.asLong == 0L) return SMono.empty[MessageChannel]

        SMono(DiscordPlugin.dc.getChannelById(id)).onErrorResume(e => {
            def foo(e: Throwable) = {
                getLogger.warning("Failed to get channel data for " + key + "=" + id + " - " + e.getMessage)
                SMono.empty
            }

            foo(e)
        }).filter(ch => ch.isInstanceOf[MessageChannel]).cast[MessageChannel]
    }

    def getMessageChannel(config: ConfigData[Snowflake]): SMono[MessageChannel] =
        getMessageChannel(config.getPath, config.get)

    def ignoreError[T](mono: SMono[T]): SMono[T] = mono.onErrorResume((_: Throwable) => SMono.empty)

    implicit class MonoExtensions[T](mono: Mono[T]) {
        def ^^(): SMono[T] = SMono(mono)
    }

    implicit class FluxExtensions[T](flux: Flux[T]) {
        def ^^(): SFlux[T] = SFlux(flux)
    }

    implicit class SpecExtensions[T <: LegacySpec[_]](spec: T) {
        def ^^(): Unit = ()
    }

}