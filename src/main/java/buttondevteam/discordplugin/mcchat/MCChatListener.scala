package buttondevteam.discordplugin.mcchat

import buttondevteam.core.ComponentManager
import buttondevteam.core.component.channel.Channel
import buttondevteam.discordplugin._
import buttondevteam.discordplugin.listeners.CommandListener
import buttondevteam.discordplugin.playerfaker.{VanillaCommandListener, VanillaCommandListener14, VanillaCommandListener15}
import buttondevteam.lib._
import buttondevteam.lib.chat.{ChatMessage, TBMCChatAPI}
import buttondevteam.lib.player.TBMCPlayer
import com.vdurmont.emoji.EmojiParser
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.{MessageChannel, PrivateChannel}
import discord4j.core.`object`.entity.{Member, Message, User}
import discord4j.core.event.domain.message.MessageCreateEvent
import discord4j.core.spec.{EmbedCreateSpec, MessageEditSpec}
import discord4j.rest.util.Color
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.{EventHandler, Listener}
import org.bukkit.scheduler.BukkitTask
import reactor.core.publisher.Mono
import reactor.core.scala.publisher.{SFlux, SMono}

import java.time.Instant
import java.util
import java.util.Optional
import java.util.concurrent.{LinkedBlockingQueue, TimeoutException}
import java.util.function.{Consumer, Function, Predicate}
import java.util.stream.Collectors

object MCChatListener {

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
    @FunctionalInterface private trait InterruptibleConsumer[T] {
        @throws[TimeoutException]
        @throws[InterruptedException]
        def accept(value: T)
    }

}

class MCChatListener(val module: MinecraftChatModule) extends Listener {
    private var sendtask: BukkitTask = null
    final private val sendevents = new LinkedBlockingQueue[util.AbstractMap.SimpleEntry[TBMCChatEvent, Instant]]
    private var sendrunnable: Runnable = null
    private var sendthread: Thread = null
    private var stop = false //A new instance will be created on enable
    @EventHandler // Minecraft
    def onMCChat(ev: TBMCChatEvent): Unit = {
        if (!(ComponentManager.isEnabled(classOf[MinecraftChatModule])) || ev.isCancelled) { //SafeMode: Needed so it doesn't restart after server shutdown
            return
        }

        sendevents.add(new util.AbstractMap.SimpleEntry[TBMCChatEvent, Instant](ev, Instant.now))
        if (sendtask != null) {
            return
        }
        sendrunnable = () => {
            def foo(): Unit = {
                sendthread = Thread.currentThread
                processMCToDiscord()
                if (DiscordPlugin.plugin.isEnabled && !(stop)) { //Don't run again if shutting down
                    sendtask = Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, sendrunnable)
                }
            }

            foo()
        }
        sendtask = Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, sendrunnable)
    }

    private def processMCToDiscord(): Unit = {
        try {
            var e: TBMCChatEvent = null
            var time: Instant = null
            val se: util.AbstractMap.SimpleEntry[TBMCChatEvent, Instant] = sendevents.take // Wait until an element is available
            e = se.getKey
            time = se.getValue
            val authorPlayer: String = "[" + DPUtils.sanitizeStringNoEscape(e.getChannel.DisplayName.get) + "] " + //
                (if ("Minecraft" == e.getOrigin) "" else "[" + e.getOrigin.charAt(0) + "]") +
                DPUtils.sanitizeStringNoEscape(ChromaUtils.getDisplayName(e.getSender))
            val color: chat.Color = e.getChannel.Color.get
            val embed: Consumer[EmbedCreateSpec] = (ecs: EmbedCreateSpec) => {
                def foo(ecs: EmbedCreateSpec) = {
                    ecs.setDescription(e.getMessage).setColor(Color.of(color.getRed, color.getGreen, color.getBlue))
                    val url: String = module.profileURL.get
                    e.getSender match {
                        case player: Player =>
                            DPUtils.embedWithHead(ecs, authorPlayer, e.getSender.getName,
                                if (url.nonEmpty) url + "?type=minecraft&id=" + player.getUniqueId else null)
                        case dsender: DiscordSenderBase =>
                            ecs.setAuthor(authorPlayer,
                                if (url.nonEmpty) url + "?type=discord&id=" + dsender.getUser.getId.asString else null,
                                dsender.getUser.getAvatarUrl)
                        case _ =>
                            DPUtils.embedWithHead(ecs, authorPlayer, e.getSender.getName, null)
                    }
                    ecs.setTimestamp(time)
                }

                foo(ecs)
            }
            val nanoTime: Long = System.nanoTime
            val doit: MCChatListener.InterruptibleConsumer[MCChatUtils.LastMsgData] = (lastmsgdata: MCChatUtils.LastMsgData) => {
                def foo(lastmsgdata: MCChatUtils.LastMsgData): Unit = {
                    if (lastmsgdata.message == null || !(authorPlayer == lastmsgdata.message.getEmbeds.get(0).getAuthor.map(_.getName).orElse(null)) || lastmsgdata.time / 1000000000f < nanoTime / 1000000000f - 120 || !(lastmsgdata.mcchannel.ID == e.getChannel.ID) || lastmsgdata.content.length + e.getMessage.length + 1 > 2048) {
                        lastmsgdata.message = lastmsgdata.channel.createEmbed(embed).block
                        lastmsgdata.time = nanoTime
                        lastmsgdata.mcchannel = e.getChannel
                        lastmsgdata.content = e.getMessage
                    }
                    else {
                        lastmsgdata.content = lastmsgdata.content + "\n" + e.getMessage // The message object doesn't get updated
                        lastmsgdata.message.edit((mes: MessageEditSpec) => mes.setEmbed(embed.andThen((ecs: EmbedCreateSpec) => ecs.setDescription(lastmsgdata.content)))).block
                    }
                }

                foo(lastmsgdata)
            }
            // Checks if the given channel is different than where the message was sent from
            // Or if it was from MC
            val isdifferentchannel: Predicate[Snowflake] = (id: Snowflake) => !((e.getSender.isInstanceOf[DiscordSenderBase])) || (e.getSender.asInstanceOf[DiscordSenderBase]).getChannel.getId.asLong != id.asLong
            if (e.getChannel.isGlobal && (e.isFromCommand || isdifferentchannel.test(module.chatChannel.get))) {
                if (MCChatUtils.lastmsgdata == null)
                    MCChatUtils.lastmsgdata = new MCChatUtils.LastMsgData(module.chatChannelMono.block, null)
                doit.accept(MCChatUtils.lastmsgdata)
            }

            for (data <- MCChatPrivate.lastmsgPerUser) {
                if ((e.isFromCommand || isdifferentchannel.test(data.channel.getId)) && e.shouldSendTo(MCChatUtils.getSender(data.channel.getId, data.user))) {
                    doit.accept(data)
                }
            }
            MCChatCustom.lastmsgCustom synchronized
            val iterator = MCChatCustom.lastmsgCustom.iterator
            while ( {
                iterator.hasNext
            }) {
                val lmd = iterator.next
                if ((e.isFromCommand || isdifferentchannel.test(lmd.channel.getId)) //Test if msg is from Discord
                    && e.getChannel.ID == lmd.mcchannel.ID //If it's from a command, the command msg has been deleted, so we need to send it
                    && e.getGroupID == lmd.groupID) { //Check if this is the group we want to test - #58
                    if (e.shouldSendTo(lmd.dcp)) { //Check original user's permissions
                        doit.accept(lmd)
                    }
                    else {
                        iterator.remove() //If the user no longer has permission, remove the connection
                        lmd.channel.createMessage("The user no longer has permission to view the channel, connection removed.").subscribe
                    }
                }
            }
        } catch {
            case ex: InterruptedException =>
                //Stop if interrupted anywhere
                sendtask.cancel()
                sendtask = null
            case ex: Exception =>
                TBMCCoreAPI.SendException("Error while sending message to Discord!", ex, module)
        }
    }

    @EventHandler def onChatPreprocess(event: TBMCChatPreprocessEvent): Unit = {
        var start: Int = -(1)
        while ( {
            (start = event.getMessage.indexOf('@', start + 1), start) != ((), -1)
        }) {
            val mid: Int = event.getMessage.indexOf('#', start + 1)
            if (mid == -1) {
                return
            }
            var end_ = event.getMessage.indexOf(' ', mid + 1)
            if (end_ == -1) {
                end_ = event.getMessage.length
            }
            val end: Int = end_
            val startF: Int = start
            val user = DiscordPlugin.dc.getUsers.filter((u) => u.getUsername.equals(event.getMessage.substring(startF + 1, mid))).filter((u) => u.getDiscriminator.equals(event.getMessage.substring(mid + 1, end))).blockFirst
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
    def stop(wait: Boolean): Unit = {
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
            case e: InterruptedException =>
                e.printStackTrace() //This thread shouldn't be interrupted
        }
    }

    private var rectask: BukkitTask = null
    final private val recevents: LinkedBlockingQueue[MessageCreateEvent] = new LinkedBlockingQueue[MessageCreateEvent]
    private var recrun: Runnable = null
    private var recthread: Thread = null

    // Discord
    def handleDiscord(ev: MessageCreateEvent): SMono[Boolean] = {
        val author = Option(ev.getMessage.getAuthor.orElse(null))
        val hasCustomChat = MCChatCustom.hasCustomChat(ev.getMessage.getChannelId)
        val prefix = DiscordPlugin.getPrefix
        SMono(ev.getMessage.getChannel)
            .filter(channel => isChatEnabled(channel, author, hasCustomChat))
            .filter(channel => !isRunningMCChatCommand(channel, ev.getMessage.getContent, prefix))
            .filterWhen(_ => CommandListener.runCommand(ev.getMessage, DiscordPlugin.plugin.commandChannel.get, mentionedonly = true)) //Allow running commands in chat channels
            .filter(channel => {
                MCChatUtils.resetLastMessage(channel)
                recevents.add(ev)
                if (rectask != null) {
                    return true
                }
                recrun = () => {
                    recthread = Thread.currentThread
                    processDiscordToMC()
                    if (DiscordPlugin.plugin.isEnabled && !(stop)) {
                        rectask = Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, recrun) //Continue message processing
                    }
                }
                rectask = Bukkit.getScheduler.runTaskAsynchronously(DiscordPlugin.plugin, recrun) //Start message processing
                return true
            }).map(_ => false).defaultIfEmpty(true)
    }

    private def isChatEnabled(channel: MessageChannel, author: Option[User], hasCustomChat: Boolean) = {
        def hasPrivateChat = channel.isInstanceOf[PrivateChannel] &&
            author.exists((u: User) => MCChatPrivate.isMinecraftChatEnabled(u.getId.asString))

        def hasPublicChat = channel.getId.asLong == module.chatChannel.get.asLong

        hasPublicChat || hasPrivateChat || hasCustomChat
    }

    private def isRunningMCChatCommand(channel: MessageChannel, content: String, prefix: Char) = {
        (channel.isInstanceOf[PrivateChannel] //Only in private chat
            && content.length < "/mcchat<>".length
            && content.replace(prefix + "", "").equalsIgnoreCase("mcchat")) //Either mcchat or /mcchat
        //Allow disabling the chat if needed
    }

    private def processDiscordToMC(): Unit = {
        var event: MessageCreateEvent = null
        try event = recevents.take
        catch {
            case _: InterruptedException =>
                rectask.cancel()
                return
        }
        val sender: User = event.getMessage.getAuthor.orElse(null)
        var dmessage: String = event.getMessage.getContent
        try {
            val dsender: DiscordSenderBase = MCChatUtils.getSender(event.getMessage.getChannelId, sender)
            val user: DiscordPlayer = dsender.getChromaUser

            for (u <- SFlux(event.getMessage.getUserMentions).toIterable()) { //TODO: Role mentions
                dmessage = dmessage.replace(u.getMention, "@" + u.getUsername) // TODO: IG Formatting
                val m: Optional[Member] = u.asMember(DiscordPlugin.mainServer.getId).onErrorResume((t: Throwable) => Mono.empty).blockOptional
                if (m.isPresent) {
                    val mm: Member = m.get
                    val nick: String = mm.getDisplayName
                    dmessage = dmessage.replace(mm.getNicknameMention, "@" + nick)
                }
            }

            for (ch <- SFlux(event.getGuild.flux).flatMap(_.getChannels).toIterable()) {
                dmessage = dmessage.replace(ch.getMention, "#" + ch.getName)
            }
            dmessage = EmojiParser.parseToAliases(dmessage, EmojiParser.FitzpatrickAction.PARSE) //Converts emoji to text- TODO: Add option to disable (resource pack?)
            dmessage = dmessage.replaceAll(":(\\S+)\\|type_(?:(\\d)|(1)_2):", ":$1::skin-tone-$2:") //Convert to Discord's format so it still shows up
            dmessage = dmessage.replaceAll("<a?:(\\S+):(\\d+)>", ":$1:") //We don't need info about the custom emojis, just display their text
            val getChatMessage: Function[String, String] = (msg: String) => //
                msg + (if (event.getMessage.getAttachments.size > 0) {
                    "\n" + event.getMessage.getAttachments.stream.map(_.getUrl).collect(Collectors.joining("\n"))
                }
                else {
                    ""
                })
            val clmd: MCChatCustom.CustomLMD = MCChatCustom.getCustomChat(event.getMessage.getChannelId)
            var react: Boolean = false
            val sendChannel: MessageChannel = event.getMessage.getChannel.block
            val isPrivate: Boolean = sendChannel.isInstanceOf[PrivateChannel]
            if (dmessage.startsWith("/")) { // Ingame command
                if (handleIngameCommand(event, dmessage, dsender, user, clmd, isPrivate)) {
                    return
                }
            }
            else { // Not a command
                react = handleIngameMessage(event, dmessage, dsender, user, getChatMessage, clmd, isPrivate)
            }
            if (react) {
                try {
                    val lmfd: Message = MCChatUtils.lastmsgfromd.get(event.getMessage.getChannelId.asLong)
                    if (lmfd != null) {
                        lmfd.removeSelfReaction(DiscordPlugin.DELIVERED_REACTION).subscribe // Remove it no matter what, we know it's there 99.99% of the time
                    }
                } catch {
                    case e: Exception =>
                        TBMCCoreAPI.SendException("An error occured while removing reactions from chat!", e, module)
                }
                MCChatUtils.lastmsgfromd.put(event.getMessage.getChannelId.asLong, event.getMessage)
                event.getMessage.addReaction(DiscordPlugin.DELIVERED_REACTION).subscribe
            }
        } catch {
            case e: Exception =>
                TBMCCoreAPI.SendException("An error occured while handling message \"" + dmessage + "\"!", e, module)
        }
    }

    private def handleIngameMessage(event: MessageCreateEvent, dmessage: String, dsender: DiscordSenderBase, user: DiscordPlayer, getChatMessage: Function[String, String], clmd: MCChatCustom.CustomLMD, isPrivate: Boolean): Boolean = {
        var react: Boolean = false
        if (dmessage.isEmpty && event.getMessage.getAttachments.size == 0 && !(isPrivate) && (event.getMessage.getType eq Message.Type.CHANNEL_PINNED_MESSAGE)) {
            val rtr: Channel.RecipientTestResult = if (clmd != null) {
                clmd.mcchannel.getRTR(clmd.dcp)
            }
            else {
                dsender.getChromaUser.channel.get.getRTR(dsender)
            }
            TBMCChatAPI.SendSystemMessage(if (clmd != null) clmd.mcchannel else dsender.getChromaUser.channel.get, rtr,
                (dsender match {
                    case player: Player =>
                        player.getDisplayName
                    case _ =>
                        dsender.getName
                }) + " pinned a message on Discord.", TBMCSystemChatEvent.BroadcastTarget.ALL)
        }
        else {
            val cmb: ChatMessage.ChatMessageBuilder = ChatMessage.builder(dsender, user, getChatMessage.apply(dmessage)).fromCommand(false)
            if (clmd != null) {
                TBMCChatAPI.SendChatMessage(cmb.permCheck(clmd.dcp).build, clmd.mcchannel)
            }
            else {
                TBMCChatAPI.SendChatMessage(cmb.build)
            }
            react = true
        }
        react
    }

    private def handleIngameCommand(event: MessageCreateEvent, dmessage: String, dsender: DiscordSenderBase, user: DiscordPlayer, clmd: MCChatCustom.CustomLMD, isPrivate: Boolean): Boolean = {
        if (!(isPrivate)) {
            event.getMessage.delete.subscribe
        }
        val cmd: String = dmessage.substring(1)
        val cmdlowercased: String = cmd.toLowerCase
        if (dsender.isInstanceOf[DiscordSender] && module.whitelistedCommands.get.stream.noneMatch((s: String) => cmdlowercased == s || cmdlowercased.startsWith(s + " "))) { // Command not whitelisted
            dsender.sendMessage("Sorry, you can only access these commands from here:\n" + module.whitelistedCommands.get.stream.map((uc: String) => "/" + uc).collect(Collectors.joining(", ")) + (if (user.getConnectedID(classOf[TBMCPlayer]) == null) {
                "\nTo access your commands, first please connect your accounts, using /connect in " + DPUtils.botmention + "\nThen y"
            }
            else {
                "\nY"
            }) + "ou can access all of your regular commands (even offline) in private chat: DM me `mcchat`!")
            return true
        }
        module.log(dsender.getName + " ran from DC: /" + cmd)
        if (dsender.isInstanceOf[DiscordSender] && runCustomCommand(dsender, cmdlowercased)) {
            return true
        }
        val channel: Channel = if (clmd == null) {
            user.channel.get
        }
        else {
            clmd.mcchannel
        }
        val ev: TBMCCommandPreprocessEvent = new TBMCCommandPreprocessEvent(dsender, channel, dmessage, if (clmd == null) {
            dsender
        }
        else {
            clmd.dcp
        })
        Bukkit.getScheduler.runTask(DiscordPlugin.plugin, //Commands need to be run sync
            () => {
                def foo(): Unit = {
                    Bukkit.getPluginManager.callEvent(ev)
                    if (ev.isCancelled) {
                        return
                    }
                    try {
                        val mcpackage: String = Bukkit.getServer.getClass.getPackage.getName
                        if (!(module.enableVanillaCommands.get)) {
                            Bukkit.dispatchCommand(dsender, cmd)
                        }
                        else {
                            if (mcpackage.contains("1_12")) {
                                VanillaCommandListener.runBukkitOrVanillaCommand(dsender, cmd)
                            }
                            else {
                                if (mcpackage.contains("1_14")) {
                                    VanillaCommandListener14.runBukkitOrVanillaCommand(dsender, cmd)
                                }
                                else {
                                    if (mcpackage.contains("1_15") || mcpackage.contains("1_16")) {
                                        VanillaCommandListener15.runBukkitOrVanillaCommand(dsender, cmd)
                                    }
                                    else {
                                        Bukkit.dispatchCommand(dsender, cmd)
                                    }
                                }
                            }
                        }
                    } catch {
                        case e: NoClassDefFoundError =>
                            TBMCCoreAPI.SendException("A class is not found when trying to run command " + cmd + "!", e, module)
                        case e: Exception =>
                            TBMCCoreAPI.SendException("An error occurred when trying to run command " + cmd + "! Vanilla commands are only supported in some MC versions.", e, module)
                    }
                }

                foo()
            })
        return true
    }

    private def runCustomCommand(dsender: DiscordSenderBase, cmdlowercased: String): Boolean = {
        if (cmdlowercased.startsWith("list")) {
            val players: util.Collection[_ <: Player] = Bukkit.getOnlinePlayers
            dsender.sendMessage("There are " + players.stream.filter(MCChatUtils.checkEssentials).count + " out of " + Bukkit.getMaxPlayers + " players online.")
            dsender.sendMessage("Players: " + players.stream.filter(MCChatUtils.checkEssentials).map(Player.getDisplayName).collect(Collectors.joining(", ")))
            return true
        }
        return false
    }
}