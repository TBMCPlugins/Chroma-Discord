import be.seeseemelk.mockbukkit.MockBukkit.{load, mock}
import buttondevteam.core.MainPlugin
import org.scalatest.flatspec.AnyFlatSpec

class DiscordTest extends AnyFlatSpec {
    "DiscordPlugin" should "boot" in {
        println("Mocking Bukkit")
        mock()
        println("Loading Core")
        load(classOf[MainPlugin], true)
    }
}
