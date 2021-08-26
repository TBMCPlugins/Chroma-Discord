package buttondevteam.discordplugin.fun

import buttondevteam.core.ComponentManager
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.{Component, ConfigData}
import com.google.common.collect.Lists
import discord4j.core.`object`.entity.channel.{GuildChannel, MessageChannel}
import discord4j.core.`object`.entity.{Guild, Message}
import discord4j.core.`object`.presence.Status
import discord4j.core.event.domain.PresenceUpdateEvent
import discord4j.core.spec.legacy.{LegacyEmbedCreateSpec, LegacyMessageCreateSpec}
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.{EventHandler, Listener}
import reactor.core.scala.publisher.{SFlux, SMono}

import java.util
import java.util.Calendar
import java.util.concurrent.TimeUnit
import java.util.stream.IntStream
import scala.util.Random

/**
 * All kinds of random things.
 * The YEEHAW event uses an emoji named :YEEHAW: if available
 */
object FunModule {
    private val serverReadyStrings = Array[String]("in one week from now", // Ali
        "between now and the heat-death of the universe.", // Ghostise
        "soon™", "ask again this time next month", "in about 3 seconds", // Nicolai
        "after we finish 8 plugins", "tomorrow.", "after one tiiiny feature",
        "next commit", "after we finish strangling Towny", "when we kill every *fucking* bug",
        "once the server stops screaming.", "after HL3 comes out", "next time you ask",
        "when will *you* be open?") // Ali
    private val serverReadyRandom = new Random
    private val usableServerReadyStrings = new java.util.ArrayList[Short](0)
    private var lastlist = 0
    private var lastlistp = 0
    private var ListC = 0

    def executeMemes(message: Message): Boolean = {
        val fm = ComponentManager.getIfEnabled(classOf[FunModule])
        if (fm == null) return false
        val msglowercased = message.getContent.toLowerCase
        lastlist += 1
        if (lastlist > 5) {
            ListC = 0
            lastlist = 0
        }
        if (msglowercased == "/list" && Bukkit.getOnlinePlayers.size == lastlistp && {
            ListC += 1
            ListC - 1
        } > 2) { // Lowered already
            DPUtils.reply(message, SMono.empty, "stop it. You know the answer.").subscribe()
            lastlist = 0
            lastlistp = Bukkit.getOnlinePlayers.size.toShort
            return true //Handled
        }
        lastlistp = Bukkit.getOnlinePlayers.size.toShort //Didn't handle
        if (!TBMCCoreAPI.IsTestServer && fm.serverReady.get.exists(msglowercased.contains)) {
            var next = 0
            if (usableServerReadyStrings.size == 0) fm.createUsableServerReadyStrings()
            next = usableServerReadyStrings.remove(serverReadyRandom.nextInt(usableServerReadyStrings.size))
            DPUtils.reply(message, SMono.empty, fm.serverReadyAnswers.get.get(next)).subscribe()
            return false //Still process it as a command/mcchat if needed
        }
        false
    }

    private var lasttime: Long = 0

    def handleFullHouse(event: PresenceUpdateEvent): Unit = {
        val fm = ComponentManager.getIfEnabled(classOf[FunModule])
        if (fm == null) return ()
        if (Calendar.getInstance.get(Calendar.DAY_OF_MONTH) % 5 != 0) return ()
        if (!Option(event.getOld.orElse(null)).exists(_.getStatus == Status.OFFLINE)
            || event.getCurrent.getStatus == Status.OFFLINE)
            return () //If it's not an offline -> online change
        fm.fullHouseChannel.get.filter((ch: MessageChannel) => ch.isInstanceOf[GuildChannel])
            .flatMap(channel => fm.fullHouseDevRole(SMono(channel.asInstanceOf[GuildChannel].getGuild)).get
                .filterWhen(devrole => SMono(event.getMember)
                    .flatMap(m => SFlux(m.getRoles).any(_.getId.asLong == devrole.getId.asLong)))
                .filterWhen(devrole => SMono(event.getGuild)
                    .flatMapMany(g => SFlux(g.getMembers).filter(_.getRoleIds.stream.anyMatch(_ == devrole.getId)))
                    .flatMap(_.getPresence).all(_.getStatus != Status.OFFLINE))
                .filter(_ => lasttime + 10 < TimeUnit.NANOSECONDS.toHours(System.nanoTime)) //This should stay so it checks this last
                .flatMap(_ => {
                    lasttime = TimeUnit.NANOSECONDS.toHours(System.nanoTime)
                    SMono(channel.createMessage(_.setContent("Full house!")
                        .setEmbed((ecs: LegacyEmbedCreateSpec) => ecs.setImage("https://cdn.discordapp.com/attachments/249295547263877121/249687682618359808/poker-hand-full-house-aces-kings-playing-cards-15553791.png"))))
                })).subscribe()
    }
}

class FunModule extends Component[DiscordPlugin] with Listener {
    /**
     * Questions that the bot will choose a random answer to give to.
     */
    final private val serverReady: ConfigData[Array[String]] =
        getConfig.getData("serverReady", () => Array[String](
            "when will the server be open", "when will the server be ready",
            "when will the server be done", "when will the server be complete",
            "when will the server be finished", "when's the server ready",
            "when's the server open", "vhen vill ze server be open?"))
    /**
     * Answers for a recognized question. Selected randomly.
     */
    final private val serverReadyAnswers: ConfigData[util.ArrayList[String]] =
        getConfig.getData("serverReadyAnswers", () => Lists.newArrayList(FunModule.serverReadyStrings: _*))

    private def createUsableServerReadyStrings(): Unit =
        IntStream.range(0, serverReadyAnswers.get.size).forEach((i: Int) => FunModule.usableServerReadyStrings.add(i.toShort))

    override protected def enable(): Unit = registerListener(this)

    override protected def disable(): Unit = {
        FunModule.lastlist = 0
        FunModule.lastlistp = 0
        FunModule.ListC = 0
    }

    @EventHandler def onPlayerJoin(event: PlayerJoinEvent): Unit = FunModule.ListC = 0

    /**
     * If all of the people who have this role are online, the bot will post a full house.
     */
    private def fullHouseDevRole(guild: SMono[Guild]) = DPUtils.roleData(getConfig, "fullHouseDevRole", "Developer", guild)

    /**
     * The channel to post the full house to.
     */
    final private val fullHouseChannel = DPUtils.channelData(getConfig, "fullHouseChannel")
}