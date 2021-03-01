package buttondevteam.discordplugin.util

import buttondevteam.discordplugin.listeners.CommonListeners

class Timings() {
    private var start = System.nanoTime

    def printElapsed(message: String): Unit = {
        CommonListeners.debug(message + " (" + (System.nanoTime - start) / 1000000L + ")")
        start = System.nanoTime
    }
}