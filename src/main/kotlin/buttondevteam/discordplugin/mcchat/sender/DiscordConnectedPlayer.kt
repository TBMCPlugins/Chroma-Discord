package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.PlayerInventory
import org.bukkit.plugin.Plugin
import org.mockito.Answers.RETURNS_DEFAULTS
import org.mockito.invocation.InvocationOnMock

import java.lang.reflect.Modifier
import java.util
import java.util._

object DiscordConnectedPlayer {
    fun create(user: User, channel: MessageChannel, uuid: UUID, mcname: String, module: MinecraftChatModule): DiscordConnectedPlayer =
        Mockito.mock(classOf[DiscordConnectedPlayer], getSettings.useConstructor(user, channel, uuid, mcname, module))

    fun createTest(): DiscordConnectedPlayer =
        Mockito.mock(classOf[DiscordConnectedPlayer], getSettings.useConstructor(null, null))

    private fun getSettings(): MockSettings = Mockito.withSettings.defaultAnswer((invocation: InvocationOnMock) -> {
        def foo(invocation: InvocationOnMock): AnyRef =
            try {
                if (!Modifier.isAbstract(invocation.getMethod.getModifiers))
                    invocation.callRealMethod
                else
                    RETURNS_DEFAULTS.answer(invocation)
            } catch {
                case e: Exception ->
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
abstract class DiscordConnectedPlayer(user: User, channel: MessageChannel, val uniqueId: UUID, val playerName: String, val module: MinecraftChatModule) : DiscordSenderBase(user, channel) with IMCPlayer[DiscordConnectedPlayer] with Player {
    private var loggedIn = false
    private var dispName: String = playerName

    private var location: Location = if (module == null) null else Bukkit.getWorlds.get(0).getSpawnLocation
    private val basePlayer: OfflinePlayer = if (module == null) null else Bukkit.getOfflinePlayer(uniqueId)
    private var perm: PermissibleBase = if (module == null) null else PermissibleBase(basePlayer)
    private val origPerm: PermissibleBase = perm
    private val vanillaCmdListener = null // TODO
    private val inventory: PlayerInventory = if (module == null) null else Bukkit.createInventory(this, InventoryType.PLAYER).asInstanceOf

    override fun isPermissionSet(name: String): Boolean = this.origPerm.isPermissionSet(name)

    override fun isPermissionSet(perm: Permission): Boolean = this.origPerm.isPermissionSet(perm)

    override fun hasPermission(inName: String): Boolean = this.origPerm.hasPermission(inName)

    override fun hasPermission(perm: Permission): Boolean = this.origPerm.hasPermission(perm)

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean): PermissionAttachment = this.origPerm.addAttachment(plugin, name, value)

    override fun addAttachment(plugin: Plugin): PermissionAttachment = this.origPerm.addAttachment(plugin)

    override fun removeAttachment(attachment: PermissionAttachment): Unit = this.origPerm.removeAttachment(attachment)

    override fun recalculatePermissions(): Unit = this.origPerm.recalculatePermissions()

    def clearPermissions(): Unit = this.origPerm.clearPermissions()

    override fun addAttachment(plugin: Plugin, name: String, value: Boolean, ticks: Int): PermissionAttachment =
        this.origPerm.addAttachment(plugin, name, value, ticks)

    override fun addAttachment(plugin: Plugin, ticks: Int): PermissionAttachment = this.origPerm.addAttachment(plugin, ticks)

    override fun getEffectivePermissions(): util.Set[PermissionAttachmentInfo] = this.origPerm.getEffectivePermissions

    def setLoggedIn(loggedIn: Boolean): Unit = this.loggedIn = loggedIn

    def setPerm(perm: PermissibleBase): Unit = this.perm = perm

    override fun setDisplayName(displayName: String): Unit = this.dispName = displayName

    override fun getVanillaCmdListener = this.vanillaCmdListener

    def isLoggedIn: Boolean = this.loggedIn

    override fun getName: String = this.playerName

    def getBasePlayer: OfflinePlayer = this.basePlayer

    def getPerm: PermissibleBase = this.perm

    override fun getUniqueId: UUID = this.uniqueId

    override fun getDisplayName(): String = this.dispName

    /**
     * For testing
     */
    def this(user: User, channel: MessageChannel) =
        this(user, channel, UUID.randomUUID(), "Test", null)

    override fun setOp(value: Boolean): Unit = { //CraftPlayer-compatible implementation
        this.origPerm.setOp(value)
        this.perm.recalculatePermissions()
    }

    override fun isOp(): Boolean = this.origPerm.isOp

    override fun teleport(location: Location): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = location
        true
    }

    def teleport(location: Location, cause: PlayerTeleportEvent.TeleportCause): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = location
        true
    }

    override fun teleport(destination: Entity): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = destination.getLocation
        true
    }

    def teleport(destination: Entity, cause: PlayerTeleportEvent.TeleportCause): Boolean = {
        if (module.allowFakePlayerTeleports.get) this.location = destination.getLocation
        true
    }

    override fun getLocation(loc: Location): Location = {
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

    override fun getServer(): Server = Bukkit.getServer

    override fun sendRawMessage(message: String): Unit = sendMessage(message)

    override fun chat(msg: String): Unit = Bukkit.getPluginManager.callEvent(AsyncPlayerChatEvent(true, this, msg, util.HashSet[Player](Bukkit.getOnlinePlayers)))

    override fun getWorld(): World = Bukkit.getWorlds.get(0)

    override fun isOnline = true

    override fun getLocation = Location(getWorld, location.getX, location.getY, location.getZ, location.getYaw, location.getPitch)

    override fun getEyeLocation: Location = getLocation

    @deprecated override fun getMaxHealth = 20d

    override fun getPlayer(): DiscordConnectedPlayer = this

    override fun getAttribute(attribute: Attribute): AttributeInstance = AttributeInstance() {
        override fun getAttribute: Attribute = attribute

        override fun getBaseValue(): Double = getDefaultValue

        override fun setBaseValue(value: Double): Unit = {
        }

        override fun getModifiers(): util.Collection[AttributeModifier] = Collections.emptyList

        override fun addModifier(modifier: AttributeModifier): Unit = {
        }

        override fun removeModifier(modifier: AttributeModifier): Unit = {
        }

        override fun getValue: Double = getDefaultValue

        override fun getDefaultValue: Double = 20 //Works for max health, should be okay for the rest
    }

    override fun getGameMode = GameMode.SPECTATOR

    override fun getInventory(): PlayerInventory = inventory

    override fun name(): Component = Component.text(playerName)

    //noinspection ScalaDeprecation
    /*@SuppressWarnings(arrayOf("deprecation")) override fun spigot(): super.Spigot = super.Spigot() {
        override fun getRawAddress(): InetSocketAddress = null

        override fun playEffect(location: Location, effect: Effect, id: Int, data: Int, offsetX: Float, offsetY: Float, offsetZ: Float, speed: Float, particleCount: Int, radius: Int): Unit = {
        }

        override fun getCollidesWithEntities = false

        override fun setCollidesWithEntities(collides: Boolean): Unit = {
        }

        override fun respawn(): Unit = {
        }

        override fun getLocale = "en_us"

        override fun getHiddenPlayers(): util.Set[Player] = Collections.emptySet

        override fun sendMessage(component: BaseComponent): Unit =
            DiscordConnectedPlayer.super.sendMessage(component.toPlainText)

        override fun sendMessage(components: BaseComponent*): Unit =
            for (component <- components)
                sendMessage(component)

        override fun sendMessage(position: ChatMessageType, component: BaseComponent): Unit =
            sendMessage(component) //Ignore position
        override fun sendMessage(position: ChatMessageType, components: BaseComponent*): Unit =
            sendMessage(components: _*)

        override fun isInvulnerable = true
    }*/
}