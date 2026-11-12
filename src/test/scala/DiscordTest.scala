import be.seeseemelk.mockbukkit.MockBukkit.{load, mock}
import buttondevteam.core.MainPlugin
import buttondevteam.discordplugin.DiscordPlugin
import org.bukkit.configuration.file.YamlConfiguration
import org.scalatest.flatspec.AnyFlatSpec

class DiscordTest extends AnyFlatSpec {
    "DiscordPlugin" should "boot" in {
        println("Mocking Bukkit")
        mock()
        //new YamlConfiguration().set("asd", "dsa")
        println("Loading Core")
        load(MainPlugin::class.java, true)
        load(DiscordPlugin::class.java)
    }
}
