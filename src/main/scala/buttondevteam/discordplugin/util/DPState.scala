package buttondevteam.discordplugin.util

object DPState extends Enumeration {
    type DPState = Value
    val

    /**
     * Used from server start until anything else happens
     */
    RUNNING,

    /**
     * Used when /restart is detected
     */
    RESTARTING_SERVER,

    /**
     * Used when the plugin is disabled by outside forces
     */
    STOPPING_SERVER,

    /**
     * Used when /discord restart is run
     */
    RESTARTING_PLUGIN,

    /**
     * Used when the plugin is in the RUNNING state when the chat is disabled
     */
    DISABLED_MCCHAT = Value
}