package buttondevteam.discordplugin

import buttondevteam.discordplugin.playerfaker.VCMDWrapper
import org.bukkit.entity.Player

trait IMCPlayer[T] extends Player {
    def getVanillaCmdListener: VCMDWrapper
}