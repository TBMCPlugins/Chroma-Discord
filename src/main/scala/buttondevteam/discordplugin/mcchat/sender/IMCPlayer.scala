package buttondevteam.discordplugin.mcchat.sender

import buttondevteam.discordplugin.mcchat.playerfaker.VCMDWrapper
import org.bukkit.entity.Player

trait IMCPlayer[T] extends Player {
    def getVanillaCmdListener: VCMDWrapper
}