package buttondevteam.discordplugin

import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.playerfaker.{DiscordInventory, VCMDWrapper}
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit._
import org.bukkit.attribute.{Attribute, AttributeInstance, AttributeModifier}
import org.bukkit.entity.Player.Spigot
import org.bukkit.entity.{Entity, Player}
import org.bukkit.event.player.{AsyncPlayerChatEvent, PlayerTeleportEvent}
import org.bukkit.inventory.{Inventory, PlayerInventory}
import org.bukkit.permissions.{PermissibleBase, Permission, PermissionAttachment, PermissionAttachmentInfo}
import org.bukkit.plugin.Plugin
import org.mockito.Answers.RETURNS_DEFAULTS
import org.mockito.{MockSettings, Mockito}
import org.mockito.invocation.InvocationOnMock

import java.lang.reflect.Modifier
import java.net.InetSocketAddress
import java.util
import java.util._

object DiscordConnectedPlayer {
    def create(user: User, channel: MessageChannel, uuid: UUID, mcname: String, module: MinecraftChatModule): DiscordConnectedPlayer =
        Mockito.mock(classOf[DiscordConnectedPlayer], getSettings.useConstructor(user, channel, uuid, mcname, module))

    def createTest: DiscordConnectedPlayer =
        Mockito.mock(classOf[DiscordConnectedPlayer], getSettings.useConstructor(null, null))

    private def getSettings: MockSettings = Mockito.withSettings.defaultAnswer((invocation: InvocationOnMock) => {
        def foo(invocation: InvocationOnMock): AnyRef =
            try {
                if (!Modifier.isAbstract(invocation.getMethod.getModifiers))
                    invocation.callRealMethod
                else if (classOf[PlayerInventory].isAssignableFrom(invocation.getMethod.getReturnType))
                    Mockito.mock(classOf[DiscordInventory], Mockito.withSettings.extraInterfaces(classOf[PlayerInventory]))
                else if (classOf[Inventory].isAssignableFrom(invocation.getMethod.getReturnType))
                    new DiscordInventory
                else
                    RETURNS_DEFAULTS.answer(invocation)
            } catch {
                case e: Exception =>
                    System.err.println("Error in mocked player!")
                    e.printStackTrace()
                    RETURNS_DEFAULTS.answer(invocation)
            }

        foo(invocation)
    }).stubOnly
}

abstract class DiscordConnectedPlayer(user: User, channel: MessageChannel) extends DiscordSenderBase(user, channel) with IMCPlayer[DiscordConnectedPlayer] {
    override def isPermissionSet(name: String): Boolean = this.origPerm.isPermissionSet(name)

    override def isPermissionSet(perm: Permission): Boolean = this.origPerm.isPermissionSet(perm)

    override def hasPermission(inName: String): Boolean = this.origPerm.hasPermission(inName)

    override def hasPermission(perm: Permission): Boolean = this.origPerm.hasPermission(perm)

    override def addAttachment(plugin: Plugin, name: String, value: Boolean): PermissionAttachment = this.origPerm.addAttachment(plugin, name, value)

    override def addAttachment(plugin: Plugin): PermissionAttachment = this.origPerm.addAttachment(plugin)

    override def removeAttachment(attachment: PermissionAttachment): Unit = this.origPerm.removeAttachment(attachment)

    override def recalculatePermissions(): Unit = this.origPerm.recalculatePermissions()

    def clearPermissions(): Unit = this.origPerm.clearPermissions()

    override def addAttachment(plugin: Plugin, name: String, value: Boolean, ticks: Int): PermissionAttachment =
        this.origPerm.addAttachment(plugin, name, value, ticks)

    override def addAttachment(plugin: Plugin, ticks: Int): PermissionAttachment = this.origPerm.addAttachment(plugin, ticks)

    override def getEffectivePermissions: util.Set[PermissionAttachmentInfo] = this.origPerm.getEffectivePermissions

    def setLoggedIn(loggedIn: Boolean): Unit = this.loggedIn = loggedIn

    def setPerm(perm: PermissibleBase): Unit = this.perm = perm

    override def setDisplayName(displayName: String): Unit = this.displayName = displayName

    override def getVanillaCmdListener: VCMDWrapper = this.vanillaCmdListener

    def isLoggedIn: Boolean = this.loggedIn

    override def getName: String = this.name

    def getBasePlayer: OfflinePlayer = this.basePlayer

    def getPerm: PermissibleBase = this.perm

    override def getUniqueId: UUID = this.uniqueId

    override def getDisplayName: String = this.displayName

    private var vanillaCmdListener: VCMDWrapper = null
    private var loggedIn = false
    private var origPerm: PermissibleBase = null
    private var name: String = null
    private var basePlayer: OfflinePlayer = null
    private var perm: PermissibleBase = null
    private var location: Location = null
    final private var module: MinecraftChatModule = null
    final private var uniqueId: UUID = null
    final private var displayName: String = null

    /**
     * The parameters must match with {@link #create ( User, MessageChannel, UUID, String, MinecraftChatModule)}
     */
    def this(user: User, channel: MessageChannel, uuid: UUID, mcname: String, module: MinecraftChatModule) {
        this(user, channel)
        location = Bukkit.getWorlds.get(0).getSpawnLocation
        perm = new PermissibleBase(basePlayer = Bukkit.getOfflinePlayer(uuid))
        origPerm = perm
        name = mcname
        this.module = module
        uniqueId = uuid
        displayName = mcname
        vanillaCmdListener = new VCMDWrapper(VCMDWrapper.createListener(this, module))
    }

    /**
     * For testing
     */
    def this(user: User, channel: MessageChannel) {
        this(user, channel)
        module = null
        uniqueId = UUID.randomUUID
    }

    override def setOp(value: Boolean): Unit = { //CraftPlayer-compatible implementation
        this.origPerm.setOp(value)
        this.perm.recalculatePermissions()
    }

    override def isOp: Boolean = this.origPerm.isOp

    override def teleport(location: Location): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = location
        true
    }

    def teleport(location: Location, cause: PlayerTeleportEvent.TeleportCause): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = location
        true
    }

    override def teleport(destination: Entity): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = destination.getLocation
        true
    }

    def teleport(destination: Entity, cause: PlayerTeleportEvent.TeleportCause): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = destination.getLocation
        true
    }

    override def getLocation(loc: Location): Location = {
        if (loc != null) {
            loc.setWorld(getWorld)
            loc.setX(location.getX)
            loc.setY(location.getY)
            loc.setZ(location.getZ)
            loc.setYaw(location.getYaw)
            loc.setPitch(location.getPitch)
        }
        loc
    }

    override def getServer: Server = Bukkit.getServer

    override def sendRawMessage(message: String): Unit = sendMessage(message)

    override def chat(msg: String): Unit = Bukkit.getPluginManager.callEvent(new AsyncPlayerChatEvent(true, this, msg, new util.HashSet[Player](Bukkit.getOnlinePlayers)))

    override def getWorld: World = Bukkit.getWorlds.get(0)

    override def isOnline = true

    override def getLocation = new Location(getWorld, location.getX, location.getY, location.getZ, location.getYaw, location.getPitch)

    override def getEyeLocation: Location = getLocation

    @deprecated override def getMaxHealth = 20

    override def getPlayer: DiscordConnectedPlayer = this

    override def getAttribute(attribute: Attribute): AttributeInstance = new AttributeInstance() {
        override def getAttribute: Attribute = attribute

        override def getBaseValue: Double = getDefaultValue

        override def setBaseValue(value: Double): Unit = {
        }

        override def getModifiers: util.Collection[AttributeModifier] = Collections.emptyList

        override def addModifier(modifier: AttributeModifier): Unit = {
        }

        override def removeModifier(modifier: AttributeModifier): Unit = {
        }

        override def getValue: Double = getDefaultValue

        override def getDefaultValue: Double = 20 //Works for max health, should be okay for the rest
    }

    override def getGameMode = GameMode.SPECTATOR

    //noinspection ScalaDeprecation
    @SuppressWarnings(Array("deprecation")) final private val spigot: Spigot = new Spigot() {
        override def getRawAddress: InetSocketAddress = null

        override def playEffect(location: Location, effect: Effect, id: Int, data: Int, offsetX: Float, offsetY: Float, offsetZ: Float, speed: Float, particleCount: Int, radius: Int): Unit = {
        }

        override def getCollidesWithEntities = false

        override def setCollidesWithEntities(collides: Boolean): Unit = {
        }

        override def respawn(): Unit = {
        }

        override def getLocale = "en_us"

        override def getHiddenPlayers: util.Set[Player] = Collections.emptySet

        override def sendMessage(component: BaseComponent): Unit =
            DiscordConnectedPlayer.super.sendMessage(component.toPlainText)

        override def sendMessage(components: BaseComponent*): Unit =
            for (component <- components)
                sendMessage(component)

        override def sendMessage(position: ChatMessageType, component: BaseComponent): Unit =
            sendMessage(component) //Ignore position
        override def sendMessage(position: ChatMessageType, components: BaseComponent*) =
            sendMessage(components)

        override def isInvulnerable = true
    }

    override def spigot: Spigot = spigot
}