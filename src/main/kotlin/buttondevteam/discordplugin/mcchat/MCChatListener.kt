package buttondevteam.discordplugin.mcchat

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.DPUtils
import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.discordplugin.mcchat.sender.DiscordSender
import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.lib.TBMCChatEvent
import buttondevteam.lib.TBMCChatPreprocessEvent
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.player.TBMCPlayer
import com.vdurmont.emoji.EmojiParser
import discord4j.common.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.EmbedCreateSpec
import discord4j.rest.http.client.ClientException
import discord4j.rest.util.Color
import io.netty.handler.codec.http.HttpResponseStatus
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.scheduler.BukkitTask

import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue
import java.util.function.Predicate
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
    private var sendtask: BukkitTask? = null
    private val sendevents = LinkedBlockingQueue<Pair<TBMCChatEvent, Instant>>()
    private var sendrunnable: Runnable? = null
    private var sendthread: Thread? = null
    private var stop = false //A new instance will be created on enable
    @EventHandler // Minecraft
    fun onMCChat(ev: TBMCChatEvent) {
        if (!(ComponentManager.isEnabled(MinecraftChatModule::class.java)) || ev.isCancelled) { //SafeMode: Needed so it doesn't restart after server shutdown
            return
        }

        sendevents.add(Pair<TBMCChatEvent, Instant>(ev, Instant.now()))
        if (sendtask != null) {
            return
        }
        sendrunnable = Runnable {
            sendthread = Thread.currentThread()
            processMCToDiscord()
            if (DiscordPlugin.plugin!!.isEnabled && !(stop)) { //Don't run again if shutting down
                sendtask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin!!, sendrunnable!!)
            }
        }
        sendtask = Bukkit.getScheduler().runTaskAsynchronously(DiscordPlugin.plugin!!, sendrunnable!!)
    }

    private fun processMCToDiscord() {
        try {
            val (e, time) = sendevents.take() // Wait until an element is available
            val authorPlayer: String = "[" + DPUtils.sanitizeStringNoEscape(e.channel.displayName.get()) + "] " + //
                (if ("Minecraft" == e.origin) "" else "[" + e.origin[0] + "]") +
                DPUtils.sanitizeStringNoEscape(e.user.name)
            val color: TextColor = e.channel.color
            val embed = {
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
            val doit = { lastmsgdata: MCChatUtils.LastMsgData ->
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
                    //TODO: The editing shouldn't be blocking, this whole method should be suspend fun
                    lastmsgdata.content = lastmsgdata.content + "\n" + e.message // The message object doesn't get updated
                    try {
                        message.edit().withEmbeds(embed().description(lastmsgdata.content).build()).block()
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
        } catch (ex: InterruptedException) {
            //Stop if interrupted anywhere
            sendtask?.cancel()
            sendtask = null
        } catch (ex: Exception) {
            TBMCCoreAPI.SendException("Error while sending message to Discord!", ex, module)
        }
    }

    @EventHandler fun onChatPreprocess(event: TBMCChatPreprocessEvent) {
        var start: Int = -(1)
        while ( {
            (start = event.getMessage.indexOf('@', start + 1), start) != ((), -1)
        }) {
            val mid: Int = event.getMessage.indexOf('#', start + 1)
            if (mid == -1) {
                return Unit
            }
            var end_ = event.getMessage.indexOf(' ', mid + 1)
            if (end_ == -1) {
                end_ = event.getMessage.length
            }
            val end: Int = end_
            val startF: Int = start
            val user = DiscordPlugin.dc.getUsers.filter((u) -> u.getUsername.equals(event.getMessage.substring(startF + 1, mid))).filter((u) -> u.getDiscriminator.equals(event.getMessage.substring(mid + 1, end))).blockFirst
            if (user != null) { //TODO: Nicknames
                event.setMessage(event.getMessage.substring(0, startF) + "@" + user.getUsername + (if (event.getMessage.length > end) {
                    event.getMessage.substring(end)
                }
                else {
                    ""
                })) // TODO: Add formatting
            }
            start = end // Skip any @s inside the mention
        }
    }

    /**
     * Stop the listener permanently. Enabling the module will create a new instance.
     *
     * @param wait Wait 5 seconds for the threads to stop
     */
    fun stop(wait: Boolean): Unit = {
        stop = true
        MCChatPrivate.logoutAll()
        MCChatUtils.LoggedInPlayers.clear()
        if (sendthread != null) {
            sendthread.interrupt()
        }
        if (recthread != null) {
            recthread.interrupt()
        }
        try {
            if (sendthread != null) {
                sendthread.interrupt()
                if (wait) {
                    sendthread.join(5000)
                }
            }
            if (recthread != null) {
                recthread.interrupt()
                if (wait) {
                    recthread.join(5000)
                }
            }
            MCChatUtils.lastmsgdata = null
            MCChatPrivate.lastmsgPerUser.clear()
            MCChatCustom.lastmsgCustom.clear()
            MCChatUtils.lastmsgfromd.clear()
            MCChatUtils.UnconnectedSenders.clear()
            recthread = null
            sendthread = null
        } catch {
            case e: InterruptedException ->
                e.printStackTrace() //This thread shouldn't be interrupted
        }
    }

    private var rectask: BukkitTask = null
    final private val recevents: LinkedBlockingQueue[MessageCreateEvent] = LinkedBlockingQueue[MessageCreateEvent]
    private var recrun: Runnable = null
    private var recthread: Thread = null

    // Discord
    fun handleDiscord(ev: MessageCreateEvent): SMono[Boolean] = {
        val author = Option(ev.getMessage.getAuthor.orElse(null))
        val hasCustomChat = MCChatCustom.hasCustomChat(ev.getMessage.getChannelId)
        val prefix = DiscordPlugin.getPrefix
        ev.getMessage.getChannel.^^()
            .filter(channel -> isChatEnabled(channel, author, hasCustomChat))
            .filter(channel -> !isRunningMCChatCommand(channel, ev.getMessage.getContent, prefix))
            .filter(channel -> {
                MCChatUtils.resetLastMessage(channel)
                recevents.add(ev)
                if (rectask == null) {
                    recrun = () -> {
                        recthread = Thread.currentThread
                        processDiscordToMC()
                        if (DiscordPlugin.plugin.isEnabled && !(stop)) {
                            rectask = Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, recrun) //Continue message processing
                        }
                    }
                    rectask = Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, recrun) //Start message processing
                }
                true
            }).map(_ -> false).defaultIfEmpty(true)
    }

    private fun isChatEnabled(channel: MessageChannel, author: Option[User], hasCustomChat: Boolean) = {
        fun hasPrivateChat = channel.isInstanceOf[PrivateChannel] &&
            author.exists((u: User) -> MCChatPrivate.isMinecraftChatEnabled(u.getId.asString))

        fun hasPublicChat = channel.getId.asLong == module.chatChannel.get.asLong

        hasPublicChat || hasPrivateChat || hasCustomChat
    }

    private fun isRunningMCChatCommand(channel: MessageChannel, content: String, prefix: Char) = {
        (channel.isInstanceOf[PrivateChannel] //Only in private chat
            && content.length < "/mcchat<>".length
            && content.replace(prefix + "", "").equalsIgnoreCase("mcchat")) //Either mcchat or /mcchat
        //Allow disabling the chat if needed
    }

    private fun processDiscordToMC(): Unit = {
        var event: MessageCreateEvent = null
        try event = recevents.take
        catch {
            case _: InterruptedException ->
                rectask.cancel()
                return Unit
        }
        val sender: User = event.getMessage.getAuthor.orElse(null)
        var dmessage: String = event.getMessage.getContent
        try {
            val dsender: DiscordSenderBase = MCChatUtils.getSender(event.getMessage.getChannelId, sender)
            val user: DiscordUser = dsender.getChromaUser

            fun replaceUserMentions(): Unit = {
                for (u <- event.getMessage.getUserMentions.asScala) { //TODO: Role mentions
                    dmessage = dmessage.replace(u.getMention, "@" + u.getUsername) // TODO: IG Formatting
                    val m = u.asMember(DiscordPlugin.mainServer.getId).^^().onErrorResume(_ -> SMono.empty).blockOption()
                    if (m.nonEmpty) {
                        val mm: Member = m.get
                        val nick: String = mm.getDisplayName
                        dmessage = dmessage.replace(mm.getNicknameMention, "@" + nick)
                    }
                }
            }

            replaceUserMentions()

            fun replaceChannelMentions(): Unit = {
                for (ch <- event.getGuild.flux.flatMap(_.getChannels).toIterable().asScala) {
                    dmessage = dmessage.replace(ch.getMention, "#" + ch.getName)
                }
            }

            replaceChannelMentions()

            fun replaceEmojis(): Unit = {
                dmessage = EmojiParser.parseToAliases(dmessage, EmojiParser.FitzpatrickAction.PARSE) //Converts emoji to text- TODO: Add option to disable (resource pack?)
                dmessage = dmessage.replaceAll(":(\\S+)\\|type_(?:(\\d)|(1)_2):", ":$1::skin-tone-$2:") //Convert to Discord's format so it still shows up
                dmessage = dmessage.replaceAll("<a?:(\\S+):(\\d+)>", ":$1:") //We don't need info about the custom emojis, just display their text
            }

            replaceEmojis()
            val clmd = MCChatCustom.getCustomChat(event.getMessage.getChannelId)
            val sendChannel = event.getMessage.getChannel.block
            val isPrivate = sendChannel.isInstanceOf[PrivateChannel]

            fun addCheckmark() = {
                try {
                    val lmfd = MCChatUtils.lastmsgfromd.get(event.getMessage.getChannelId.asLong)
                    if (lmfd != null)
                        lmfd.removeSelfReaction(DiscordPlugin.DELIVERED_REACTION).subscribe // Remove it no matter what, we know it's there 99.99% of the time
                } catch {
                    case e: Exception ->
                        TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e, module)
                }
                MCChatUtils.lastmsgfromd.put(event.getMessage.getChannelId.asLong, event.getMessage)
                event.getMessage.addReaction(DiscordPlugin.DELIVERED_REACTION).subscribe()
            }

            if (dmessage.startsWith("/")) // Ingame command
                handleIngameCommand(event, dmessage, dsender, user, clmd, isPrivate)
            else if (handleIngameMessage(event, dmessage, dsender, user, clmd, isPrivate)) // Not a command
                addCheckmark()
        } catch {
            case e: Exception ->
                TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e, module)
        }
    }

    /**
     * Handles a message coming from Discord to Minecraft.
     *
     * @param event     The Discord event
     * @param dmessage  The message itself
     * @param dsender   The sender who sent it
     * @param user      The Chroma user of the sender
     * @param clmd      Custom chat last message data (if in a custom chat)
     * @param isPrivate Whether the chat is private
     * @return Whether the bot should react with a checkmark
     */
    private fun handleIngameMessage(event: MessageCreateEvent, dmessage: String, dsender: DiscordSenderBase, user: DiscordUser,
                                    clmd: MCChatCustom.CustomLMD, isPrivate: Boolean): Boolean = {
        fun getAttachmentText = {
            val att = event.getMessage.getAttachments.asScala
            if (att.nonEmpty) att map (_.getUrl) joinToString "\n"
            else ""
        }

        if (event.getMessage.getType eq Message.Type.CHANNEL_PINNED_MESSAGE) {
            val mcchannel = if (clmd != null) clmd.mcchannel else dsender.getChromaUser.getChannel.get
            val rtr = mcchannel getRTR (if (clmd != null) clmd.dcp.getChromaUser else user)
            TBMCChatAPI.SendSystemMessage(mcchannel, rtr, (dsender match {
                case player: Player -> player.getDisplayName
                case _ -> dsender.getName
            }) + " pinned a message on Discord.", TBMCSystemChatEvent.BroadcastTarget.ALL)
            false
        }
        else {
            val cmb = ChatMessage.builder(user, dmessage + getAttachmentText).fromCommand(false)
            if (clmd != null)
                TBMCChatAPI.sendChatMessage(cmb.permCheck(clmd.dcp.getChromaUser).build, clmd.mcchannel)
            else
                TBMCChatAPI.sendChatMessage(cmb.build)
            true
        }
    }

    /**
     * Handle a Minecraft command coming from Discord.
     *
     * @param event     The Discord event
     * @param dmessage  The Discord mewsage, starting with a slash
     * @param dsender   The sender who sent it
     * @param user      The Chroma user of the sender
     * @param clmd      The custom last message data (if in a custom chat)
     * @param isPrivate Whether the chat is private
     * @return
     */
    private fun handleIngameCommand(event: MessageCreateEvent, dmessage: String, dsender: DiscordSenderBase, user: DiscordUser,
                                    clmd: MCChatCustom.CustomLMD, isPrivate: Boolean): Unit = {
        fun notWhitelisted(cmd: String) = module.whitelistedCommands.get.stream
            .noneMatch(s -> cmd == s || cmd.startsWith(s + " "))

        fun whitelistedCommands = module.whitelistedCommands.get.asScala.map("/" + _).joinToString(", ")

        if (!isPrivate)
            event.getMessage.delete.subscribe()
        val cmd = dmessage.substring(1)
        val cmdlowercased = cmd.toLowerCase
        if (dsender.isInstanceOf[DiscordSender] && notWhitelisted(cmdlowercased)) { // Command not whitelisted
            dsender.sendMessage("Sorry, you can only access these commands from here:\n" + whitelistedCommands +
                (if (user.getConnectedID(TBMCPlayer::class.java) == null)
                    "\nTo access your commands, first please connect your accounts, using /connect in " + DPUtils.botmention
                        + "\nThen y" else "\nY") + "ou can access all of your regular commands (even offline) in private chat: DM me `mcchat`!")
            return Unit
        }
        module.log(dsender.getName + " ran from DC: /" + cmd)
        if (dsender.isInstanceOf[DiscordSender] && runCustomCommand(dsender, cmdlowercased)) {
            return Unit
        }
        val channel = if (clmd == null) user.getChannel.get else clmd.mcchannel
        val ev = TBMCCommandPreprocessEvent(user, channel, dmessage, if (clmd == null) user else clmd.dcp.getChromaUser)
        Bukkit.getScheduler.runTask(DiscordPlugin.plugin, () -> { //Commands need to be run sync
            Bukkit.getPluginManager.callEvent(ev)
            if (!ev.isCancelled)
                runMCCommand(dsender, cmd)
        })
    }

    private fun runMCCommand(dsender: DiscordSenderBase, cmd: String): Unit = {
        try {
            val mcpackage = Bukkit.getServer.getClass.getPackage.getName
            if (!module.enableVanillaCommands.get)
                Bukkit.dispatchCommand(dsender, cmd)
            else // TODO: Vanilla command handling
                Bukkit.dispatchCommand(dsender, cmd)
        } catch {
            case e: NoClassfunFoundError ->
                TBMCCoreAPI.SendException("A class is not found when trying to run command " + cmd + "!", e, module)
            case e: Exception ->
                TBMCCoreAPI.SendException("An error occurred when trying to run command " + cmd + "! Vanilla commands are only supported in some MC versions.", e, module)
        }
    }

    /**
     * Handles custom public commands. Used to hide sensitive information in public chats.
     *
     * @param dsender       The Discord sender
     * @param cmdlowercased The command, lowercased
     * @return Whether the command was a custom command
     */
    private fun runCustomCommand(dsender: DiscordSenderBase, cmdlowercased: String): Boolean = {
        if (cmdlowercased.startsWith("list")) {
            val players = Bukkit.getOnlinePlayers.asScala
            dsender.sendMessage("There are " + players.count(MCChatUtils.checkEssentials) + " out of " + Bukkit.getMaxPlayers + " players online.")
            dsender.sendMessage("Players: " + players.filter(MCChatUtils.checkEssentials).map(_.getDisplayName).joinToString(", "))
            true
        }
        else false
    }
}