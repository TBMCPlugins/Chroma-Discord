package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlayer
import buttondevteam.lib.chat.{Command2, CommandClass}
import buttondevteam.lib.player.{TBMCPlayer, TBMCPlayerBase}
import com.google.common.collect.HashBiMap
import org.bukkit.Bukkit
import org.bukkit.entity.Player

@CommandClass(helpText = Array("Connect command", //
    "This command lets you connect your account with a Minecraft account." +
        " This allows using the private Minecraft chat and other things.")) object ConnectCommand {
    /**
     * Key: Minecraft name<br>
     * Value: Discord ID
     */
    var WaitingToConnect: HashBiMap[String, String] = HashBiMap.create
}

@CommandClass(helpText = Array("Connect command",
    "This command lets you connect your account with a Minecraft account." +
        " This allows using the private Minecraft chat and other things.")) class ConnectCommand extends ICommand2DC {
    @Command2.Subcommand def `def`(sender: Command2DCSender, Minecraftname: String): Boolean = {
        val message = sender.getMessage
        val channel = message.getChannel.block
        val author = message.getAuthor.orElse(null)
        if (author == null || channel == null) return true
        if (ConnectCommand.WaitingToConnect.inverse.containsKey(author.getId.asString)) {
            channel.createMessage("Replacing " + ConnectCommand.WaitingToConnect.inverse.get(author.getId.asString) + " with " + Minecraftname).subscribe()
            ConnectCommand.WaitingToConnect.inverse.remove(author.getId.asString)
        }
        //noinspection ScalaDeprecation
        val p = Bukkit.getOfflinePlayer(Minecraftname)
        if (p == null) {
            channel.createMessage("The specified Minecraft player cannot be found").subscribe()
            return true
        }
        val pl = TBMCPlayerBase.getPlayer(p.getUniqueId, classOf[TBMCPlayer])
        val dp = pl.getAs(classOf[DiscordPlayer])
        if (dp != null && author.getId.asString == dp.getDiscordID) {
            channel.createMessage("You already have this account connected.").subscribe()
            return true
        }
        ConnectCommand.WaitingToConnect.put(p.getName, author.getId.asString)
        channel.createMessage("Alright! Now accept the connection in Minecraft from the account " + Minecraftname + " before the next server restart. You can also adjust the Minecraft name you want to connect to by running this command again.").subscribe()
        if (p.isOnline) p.asInstanceOf[Player].sendMessage("§bTo connect with the Discord account " + author.getUsername + "#" + author.getDiscriminator + " do /discord accept")
        true
    }
}