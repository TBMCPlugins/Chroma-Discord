package buttondevteam.discordplugin.mcchat.sender

import org.bukkit.entity.Player

trait IMCPlayer[T] : Player {
    fun getVanillaCmdListener(): Null // TODO
}