package buttondevteam.discordplugin.announcer

import buttondevteam.discordplugin.{DPUtils, DiscordPlayer, DiscordPlugin}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.{Component, ComponentMetadata}
import buttondevteam.lib.player.ChromaGamerBase
import com.google.gson.JsonParser
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.MessageChannel
import reactor.core.publisher.Flux

/**
 * Posts new posts from Reddit to the specified channel(s). It will pin the regular posts (not the mod posts).
 */
@ComponentMetadata(enabledByDefault = false) object AnnouncerModule {
    private var stop = false
}

@ComponentMetadata(enabledByDefault = false) class AnnouncerModule extends Component[DiscordPlugin] {
    /**
     * Channel to post new posts.
     */
    final val channel = DPUtils.channelData(getConfig, "channel")
    /**
     * Channel where distinguished (moderator) posts go.
     */
    final private val modChannel = DPUtils.channelData(getConfig, "modChannel")
    /**
     * Automatically unpins all messages except the last few. Set to 0 or >50 to disable
     */
    final private val keepPinned = getConfig.getData("keepPinned", 40.toShort)
    final private val lastAnnouncementTime = getConfig.getData("lastAnnouncementTime", 0L)
    final private val lastSeenTime = getConfig.getData("lastSeenTime", 0L)
    /**
     * The subreddit to pull the posts from
     */
    final private val subredditURL = getConfig.getData("subredditURL", "https://www.reddit.com/r/ChromaGamers")

    override protected def enable(): Unit = {
        if (DPUtils.disableIfConfigError(this, channel, modChannel)) return
        AnnouncerModule.stop = false //If not the first time
        val kp = keepPinned.get
        if (kp eq 0) return
        val msgs: Flux[Message] = channel.get.flatMapMany(_.getPinnedMessages).takeLast(kp)
        msgs.subscribe(_.unpin)
        new Thread(() => this.AnnouncementGetterThreadMethod()).start()
    }

    override protected def disable(): Unit = AnnouncerModule.stop = true

    private def AnnouncementGetterThreadMethod(): Unit = while ( {
        !AnnouncerModule.stop
    }) {
        try {
            if (!isEnabled) { //noinspection BusyWait
                Thread.sleep(10000)
                continue //todo: continue is not supported
            }
            val body = TBMCCoreAPI.DownloadString(subredditURL.get + "/new/.json?limit=10")
            val json = new JsonParser().parse(body).getAsJsonObject.get("data").getAsJsonObject.get("children").getAsJsonArray
            val msgsb = new StringBuilder
            val modmsgsb = new StringBuilder
            var lastanntime = lastAnnouncementTime.get
            for (i <- json.size - 1 to 0 by -1) {
                val item = json.get(i).getAsJsonObject
                val data = item.get("data").getAsJsonObject
                var author = data.get("author").getAsString
                val distinguishedjson = data.get("distinguished")
                var distinguished = null
                if (distinguishedjson.isJsonNull) distinguished = null
                else distinguished = distinguishedjson.getAsString
                val permalink = "https://www.reddit.com" + data.get("permalink").getAsString
                val date = data.get("created_utc").getAsLong
                if (date > lastSeenTime.get) lastSeenTime.set(date)
                else if (date > lastAnnouncementTime.get) { //noinspection ConstantConditions
                    do {
                        val reddituserclass = ChromaGamerBase.getTypeForFolder("reddit")
                        if (reddituserclass == null) break //todo: break is not supported
                        val user = ChromaGamerBase.getUser(author, reddituserclass)
                        val id = user.getConnectedID(classOf[DiscordPlayer])
                        if (id != null) author = "<@" + id + ">"
                    } while ( {
                        false
                    })
                    if (!author.startsWith("<")) author = "/u/" + author
                    (if (distinguished != null && distinguished == "moderator") modmsgsb
                    else msgsb).append("A new post was submitted to the subreddit by ").append(author).append("\n").append(permalink).append("\n")
                    lastanntime = date
                }
            }
            if (msgsb.length > 0) channel.get.flatMap((ch: MessageChannel) => ch.createMessage(msgsb.toString)).flatMap(Message.pin).subscribe
            if (modmsgsb.length > 0) modChannel.get.flatMap((ch: MessageChannel) => ch.createMessage(modmsgsb.toString)).flatMap(Message.pin).subscribe
            if (lastAnnouncementTime.get ne lastanntime) lastAnnouncementTime.set(lastanntime) // If sending succeeded} catch {
            case e: Exception =>
                e.printStackTrace()
        }
        try Thread.sleep(10000)
        catch {
            case ex: InterruptedException =>
                Thread.currentThread.interrupt()
        }
    }
}