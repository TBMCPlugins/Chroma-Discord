package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.mcchat.sender.DiscordUser
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.CommandClass
import buttondevteam.lib.player.TBMCPlayer
import buttondevteam.lib.player.TBMCPlayerBase
import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandClass(
    helpText = [
        "Connect command",
        "This command lets you connect your account with a Minecraft account. This allows using the private Minecraft chat and other things."
    ]
)
class ConnectCommand : ICommand2DC() {
    companion object {
        /**
         * Key: Minecraft name<br>
         * Value: Discord ID
         */
        var WaitingToConnect: HashBiMap<String, String> = HashBiMap.create()
    }

    @Command2.Subcommand
    fun def(sender: Command2DCSender, Minecraftname: String): Boolean {
        val author = sender.event.interaction.user
        println("Author is " + author.username)
        if (WaitingToConnect.inverse().containsKey(author.id.asString())) {
            sender.event.reply("Replacing " + WaitingToConnect.inverse()[author.id.asString()] + " with " + Minecraftname)
                .subscribe()
            WaitingToConnect.inverse().remove(author.id.asString())
        }
        val p = Bukkit.getOfflinePlayer(Minecraftname)
        if (p == null) {
            sender.event.reply("The specified Minecraft player cannot be found").subscribe()
            return true
        }
        val pl = TBMCPlayerBase.getPlayer(p.uniqueId, TBMCPlayer::class.java)
        val dp = pl.getAs(DiscordUser::class.java)
        if (dp != null && author.id.asString() == dp.discordID) {
            sender.event.reply("You already have this account connected.").subscribe()
            return true
        }
        WaitingToConnect[p.name] = author.id.asString()
        sender.event.reply("Alright! Now accept the connection in Minecraft from the account $Minecraftname before the next server restart. You can also adjust the Minecraft name you want to connect to by running this command again.")
            .subscribe()
        if (p is Player) p.sendMessage("§bTo connect with the Discord account " + author.username + "#" + author.discriminator + " do /discord accept")
        return true
    }
}