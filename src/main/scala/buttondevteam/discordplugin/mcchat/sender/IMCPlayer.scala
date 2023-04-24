package buttondevteam.discordplugin.mcchat.sender

import org.bukkit.entity.Player

trait IMCPlayer[T] extends Player {
    def getVanillaCmdListener: Null // TODO
}