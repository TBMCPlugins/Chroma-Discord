package buttondevteam.discordplugin.broadcaster

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.TBMCCoreAPI
import buttondevteam.lib.architecture.{Component, ComponentMetadata}

/**
 * Uses a bit of a hacky method of getting all broadcasted messages, including advancements and any other message that's for everyone.
 * If this component is enabled then these messages will show up on Discord.
 */
@ComponentMetadata(enabledByDefault = false) object GeneralEventBroadcasterModule {
    def isHooked: Boolean = GeneralEventBroadcasterModule.hooked

    private var hooked = false
}

@ComponentMetadata(enabledByDefault = false) class GeneralEventBroadcasterModule extends Component[DiscordPlugin] {
    override protected def enable(): Unit = try {
        // TODO: Removed for now
    } catch {
        case e: Exception =>
            TBMCCoreAPI.SendException("Error while hacking the player list! Disable this module if you're on an incompatible version.", e, this)
        case _: NoClassDefFoundError =>
            logWarn("Error while hacking the player list! Disable this module if you're on an incompatible version.")
    }

    override protected def disable(): Unit = try {
        if (!GeneralEventBroadcasterModule.hooked) return ()
    } catch {
        case e: Exception =>
            TBMCCoreAPI.SendException("Error while hacking the player list!", e, this)
        case _: NoClassDefFoundError =>
    }
}