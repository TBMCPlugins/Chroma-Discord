package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.attribute.{Attribute, AttributeInstance, AttributeModifier}
import org.bukkit.entity.{Entity, Player}
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.{AsyncPlayerChatEvent, PlayerTeleportEvent}
import org.bukkit.inventory.{Inventory, PlayerInventory}
import org.bukkit.permissions.{PermissibleBase, Permission, PermissionAttachment, PermissionAttachmentInfo}
import org.bukkit.plugin.Plugin
import org.mockito.Answers.RETURNS_DEFAULTS
import org.mockito.invocation.InvocationOnMock
import org.mockito.{MockSettings, Mockito}

import java.lang.reflect.Modifier
import java.util
import java.util.*

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

/**
 * @constructor The parameters must match with [[DiscordConnectedPlayer.create]]
 * @param user     May be null.
 * @param channel  May not be null.
 * @param uniqueId The UUID of the player.
 * @param playerName     The Minecraft name of the player.
 * @param module   The MinecraftChatModule or null if testing.
 */
abstract class DiscordConnectedPlayer(user: User, channel: MessageChannel, val uniqueId: UUID, val playerName: String, val module: MinecraftChatModule) extends DiscordSenderBase(user, channel) with IMCPlayer[DiscordConnectedPlayer] {
    private var loggedIn = false
    private var displayName: String = playerName

    private var location: Location = if (module == null) null else Bukkit.getWorlds.get(0).getSpawnLocation
    private val basePlayer: OfflinePlayer = if (module == null) null else Bukkit.getOfflinePlayer(uniqueId)
    private var perm: PermissibleBase = if (module == null) null else new PermissibleBase(basePlayer)
    private val origPerm: PermissibleBase = perm
    private val vanillaCmdListener = null // TODO
    private val inventory: PlayerInventory = if (module == null) null else Bukkit.createInventory(this, InventoryType.PLAYER).asInstanceOf

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

    override def getVanillaCmdListener = this.vanillaCmdListener

    def isLoggedIn: Boolean = this.loggedIn

    override def getName: String = this.playerName

    def getBasePlayer: OfflinePlayer = this.basePlayer

    def getPerm: PermissibleBase = this.perm

    override def getUniqueId: UUID = this.uniqueId

    override def getDisplayName: String = this.displayName

    /**
     * For testing
     */
    def this(user: User, channel: MessageChannel) =
        this(user, channel, UUID.randomUUID(), "Test", null)

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

    @deprecated override def getMaxHealth = 20d

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

    override def getInventory: PlayerInventory = inventory

    override def name(): Component = Component.text(playerName)

    //noinspection ScalaDeprecation
    /*@SuppressWarnings(Array("deprecation")) override def spigot: super.Spigot = new super.Spigot() {
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
        override def sendMessage(position: ChatMessageType, components: BaseComponent*): Unit =
            sendMessage(components: _*)

        override def isInvulnerable = true
    }*/
}