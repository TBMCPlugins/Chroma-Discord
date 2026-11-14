package buttondevteam.discordplugin.mcchat

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordSender
import buttondevteam.discordplugin.mcchat.sender.DiscordSenderBase
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.lib.*
import buttondevteam.lib.chat.ChatMessage
import buttondevteam.lib.chat.TBMCChatAPI
import buttondevteam.lib.player.TBMCPlayer
import com.vdurmont.emoji.EmojiParser
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.`object`.emoji.Emoji
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Color
import io.netty.handler.codec.http.HttpResponseStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import reactor.core.publisher.Mono
import java.time.Instant
import java.util.Locale.getDefault
import kotlin.jvm.optionals.getOrNull

// ......................DiscordSender....DiscordConnectedPlayer.DiscordPlayerSender
// Offline public chat......x............................................
// Online public chat.......x...........................................x
// Offline private chat.....x.......................x....................
// Online private chat......x.......................x...................x
// If online and enabling private chat, don't login
// If leaving the server and private chat is enabled (has ConnectedPlayer), call login in a task on lowest priority
// If private chat is enabled and joining the server, logout the fake player on highest priority
// If online and disabling private chat, don't logout
// The maps may not contain the senders for UnconnectedSenders

class MCChatListener(val module: MinecraftChatModule) : Listener {
    private val DELIVERED_REACTION = Emoji.unicode("✅")

    private var stop = false //A new instance will be created on enable
    @EventHandler // Minecraft
    suspend fun onMCChat(ev: TBMCChatEvent) {
        if (!(ComponentManager.isEnabled(MinecraftChatModule::class.java)) || ev.isCancelled) { //SafeMode: Needed so it doesn't restart after server shutdown
            return
        }

        processMCToDiscord(ev, Instant.now())
    }

    private suspend fun processMCToDiscord(e: TBMCChatEvent, time: Instant) {
        try {
            val authorPlayer: String = "[" + DPUtils.sanitizeStringNoEscape(e.channel.displayName) + "] " + //
                (if ("Minecraft" == e.origin) "" else "[" + e.origin[0] + "]") +
                DPUtils.sanitizeStringNoEscape(e.user.name)
            val color: TextColor = e.channel.color
            val embed = suspend {
                val ecs = EmbedCreateSpec.builder()
                ecs.description(e.message).color(Color.of(color.red(), color.green(), color.blue()))
                val url: String = module.profileURL
                when (val user = e.user) {
                    is TBMCPlayer ->
                        DPUtils.embedWithHead(ecs, authorPlayer, user.name,
                            if (url.isNotEmpty()) url + "?type=minecraft&id=" + user.uniqueId else null)
                    is DiscordUser ->
                        ecs.author(authorPlayer,
                            if (url.isNotEmpty()) url + "?type=discord&id=" + user.discordID else null,
                            user.getDiscordUser().avatarUrl)
                    else ->
                        DPUtils.embedWithHead(ecs, authorPlayer, e.user.name, null)
                }
                ecs.timestamp(time)
            }
            val nanoTime: Long = System.nanoTime()
            val doit = suspend { lastmsgdata: MCChatUtils.LastMsgData ->
                val message = lastmsgdata.message
                if (message == null
                    || authorPlayer != message.embeds[0].author.flatMap { it.name }.getOrNull()
                    || lastmsgdata.time / 1000000000f < nanoTime / 1000000000f - 120
                    || lastmsgdata.mcchannel?.identifier != e.channel.identifier
                    || lastmsgdata.content.length + e.message.length + 1 > 2048) {
                    lastmsgdata.message = lastmsgdata.channel.createMessage(embed().build()).block()
                    lastmsgdata.time = nanoTime
                    lastmsgdata.mcchannel = e.channel
                    lastmsgdata.content = e.message
                }
                else {
                    lastmsgdata.content = lastmsgdata.content + "\n" + e.message // The message object doesn't get updated
                    try {
                        message.edit().withEmbeds(embed().description(lastmsgdata.content).build()).asFlow().firstOrNull()
                    } catch (e: ClientException) {
                        // The message was deleted
                        if (e.status != HttpResponseStatus.NOT_FOUND) throw e
                    }
                }
            }
            // Checks if the given channel is different from where the message was sent from
            // Or if it was from MC
            val isdifferentchannel = { id: Snowflake ->
                val user = e.user
                user !is DiscordUser // TODO: We don't know what Discord channel this comes from
            }
            if (e.channel.isGlobal && (e.isFromCommand || isdifferentchannel(module.chatChannel))) {
                if (MCChatUtils.lastmsgdata == null)
                    MCChatUtils.lastmsgdata = MCChatUtils.LastMsgData(module.chatChannelMono!!, null)
                doit(MCChatUtils.lastmsgdata!!)
            }
        } catch (ex: Exception) {
            TBMCCoreAPI.SendException("Error while sending message to Discord!", ex, module)
        }
    }

    @EventHandler
    fun onChatPreprocess(event: TBMCChatPreprocessEvent) {
        // TODO: Add formatting for mentions and channels
    }

    /**
     * Stop the listener permanently. Enabling the module will create a new instance.
     */
    fun stop() {
        stop = true
        MCChatUtils.lastmsgdata = null
        MCChatUtils.lastmsgfromd.clear()
        MCChatUtils.UnconnectedSenders.clear()
    }

    // Discord
    suspend fun handleDiscord(ev: MessageCreateEvent): Boolean {
        val author = ev.message.author.orElse(null)
        val channel = ev.message.channel.asFlow().first()
        if (author == null || !isChatEnabled(channel)) return true
        MCChatUtils.resetLastMessage(channel)
        processDiscordToMC(author, ev.message)
        return false
    }

    private fun isChatEnabled(channel: MessageChannel): Boolean {
        val hasPublicChat = channel.id.asLong() == module.chatChannel.asLong()

        return hasPublicChat
    }

    private suspend fun processDiscordToMC(sender: User, message: Message) {
        var dmessage: String = message.content
        try {
            val dsender: DiscordSenderBase = MCChatUtils.getSender(message.channelId, sender)
            val user: DiscordUser = dsender.getChromaUser()

            suspend fun replaceUserMentions() {
                for (u in message.userMentions) { //TODO: Role mentions
                    dmessage = dmessage.replace(u.mention, "@" + u.username) // TODO: IG Formatting
                    val m = u.asMember(DiscordPlugin.plugin!!.mainServer!!.id).onErrorResume { _ -> Mono.empty() } .asFlow().firstOrNull()
                    if (m != null) {
                        val nick: String = m.displayName
                        dmessage = dmessage.replace(m.mention, "@$nick")
                    }
                }
            }

            replaceUserMentions()

            suspend fun replaceChannelMentions() {
                val guild = message.guild.asFlow().firstOrNull()
                if (guild != null) {
                    for (ch in guild.channels.asFlow().toList()) {
                        dmessage = dmessage.replace(ch.mention, "#" + ch.name)
                    }
                }
            }

            replaceChannelMentions()

            fun replaceEmojis() {
                dmessage = EmojiParser.parseToAliases(dmessage, EmojiParser.FitzpatrickAction.PARSE) //Converts emoji to text- TODO: Add option to disable (resource pack?)
                dmessage = dmessage.replace(Regex(":(\\S+)\\|type_(?:(\\d)|(1)_2):"), ":$1::skin-tone-$2:") //Convert to Discord's format so it still shows up
                dmessage = dmessage.replace(Regex("<a?:(\\S+):(\\d+)>"), ":$1:") //We don't need info about the custom emojis, just display their text
            }

            replaceEmojis()

            suspend fun addCheckmark() {
                try {
                    val lmfd = MCChatUtils.lastmsgfromd.get(message.channelId.asLong())
                    if (lmfd != null)
                        lmfd.removeSelfReaction(DELIVERED_REACTION).asFlow().firstOrNull() // Remove it no matter what, we know it's there 99.99% of the time
                } catch (e: Exception) {
                    TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e, module)
                }
                MCChatUtils.lastmsgfromd.put(message.channelId.asLong(), message)
                message.addReaction(DELIVERED_REACTION).asFlow().firstOrNull()
            }

            if (dmessage.startsWith("/")) // Ingame command
                handleIngameCommand(message, dmessage, dsender, user)
            else if (handleIngameMessage(message, dmessage, dsender, user)) // Not a command
                addCheckmark()
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("An error occured while handling message \"$dmessage\"!", e, module)
        }
    }

    /**
     * Handles a message coming from Discord to Minecraft.
     *
     * @param message   The Discord message
     * @param dmessage  The message contents
     * @param dsender   The sender who sent it
     * @param user      The Chroma user of the sender
     * @return Whether the bot should react with a checkmark
     */
    private fun handleIngameMessage(message: Message, dmessage: String, dsender: DiscordSenderBase, user: DiscordUser): Boolean {
        fun getAttachmentText(): String {
            val att = message.attachments
            return if (att.isNotEmpty()) att.joinToString("\n") { it.url }
            else ""
        }

        if (message.type == Message.Type.CHANNEL_PINNED_MESSAGE) {
            val mcchannel = dsender.getChromaUser().channel.get()
            val rtr = mcchannel.getRTR(user)
            TBMCChatAPI.SendSystemMessage(mcchannel, rtr, (when (dsender) {
                is Player -> dsender.displayName
                else -> dsender.name
            }) + " pinned a message on Discord.", TBMCSystemChatEvent.BroadcastTarget.ALL)
            return false
        }
        else {
            val cmb = ChatMessage.builder(user, dmessage + getAttachmentText()).fromCommand(false)
            TBMCChatAPI.sendChatMessage(cmb.build())
            return true
        }
    }

    /**
     * Handle a Minecraft command coming from Discord.
     *
     * @param message   The Discord message
     * @param dmessage  The message contents, starting with a slash
     * @param dsender   The sender who sent it
     * @param user      The Chroma user of the sender
     * @return
     */
    private suspend fun handleIngameCommand(message: Message, dmessage: String, dsender: DiscordSenderBase, user: DiscordUser) {
        fun notWhitelisted(cmd: String) = module.whitelistedCommands.none { cmd == it || cmd.startsWith("$it ") }

        val whitelistedCommands = module.whitelistedCommands.joinToString(", ") { "/$it" }

        message.delete().asFlow().firstOrNull()
        val cmd = dmessage.substring(1)
        val cmdlowercased = cmd.lowercase(getDefault())
        if (dsender is DiscordSender && notWhitelisted(cmdlowercased)) { // Command not whitelisted
            dsender.sendMessage("Sorry, you can only access these commands from here:\n$whitelistedCommands")
            return
        }
        module.log(dsender.name + " ran from DC: /" + cmd)
        // Run custom command in public channel
        if (dsender is DiscordSender && runCustomCommand(dsender, cmdlowercased)) {
            return
        }
        val channel = user.channel.get()
        val ev = TBMCCommandPreprocessEvent(user, channel, dmessage, user)
        Bukkit.getPluginManager().callEvent(ev)
        if (!ev.isCancelled)
            runMCCommand(dsender, cmd)
    }

    private fun runMCCommand(dsender: DiscordSenderBase, cmd: String) {
        try {
            // TODO: Vanilla command handling (enableVanillaCommands)
            Bukkit.dispatchCommand(dsender, cmd)
        } catch (e: Exception) {
            TBMCCoreAPI.SendException("An error occurred when trying to run command $cmd!", e, module)
        }
    }

    /**
     * Handles custom public commands. Used to hide sensitive information in public chats.
     *
     * @param dsender       The Discord sender
     * @param cmdlowercased The command, lowercased
     * @return Whether the command was a custom command
     */
    private fun runCustomCommand(dsender: DiscordSenderBase, cmdlowercased: String): Boolean {
        if (cmdlowercased.startsWith("list")) {
            val players = Bukkit.getOnlinePlayers()
            val count = players.count(MCChatUtils::checkEssentials)
            val maxPlayers = Bukkit.getMaxPlayers()
            dsender.sendMessage("There are $count out of $maxPlayers players online.")
            val playerNames = players.filter(MCChatUtils::checkEssentials).joinToString(", ") { it.displayName }
            dsender.sendMessage("Players: $playerNames")
            return true
        }
        else return false
    }
}