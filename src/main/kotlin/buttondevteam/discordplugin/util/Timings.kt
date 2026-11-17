package buttondevteam.discordplugin.util

import buttondevteam.discordplugin.listeners.CommonListeners

class Timings {
    private var start = System.nanoTime()

    fun printElapsed(message: String) {
        CommonListeners.debug(message + " (" + (System.nanoTime() - start) / 1000000L + ")")
        start = System.nanoTime()
    }
}