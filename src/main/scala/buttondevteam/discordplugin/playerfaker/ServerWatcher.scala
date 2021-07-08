package buttondevteam.discordplugin.playerfaker

import buttondevteam.discordplugin.DiscordConnectedPlayer
import buttondevteam.discordplugin.mcchat.MCChatUtils
import com.destroystokyo.paper.profile.CraftPlayerProfile
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding
import org.bukkit.entity.Player
import org.bukkit.{Bukkit, Server}
import org.mockito.Mockito
import org.mockito.internal.creation.bytebuddy.InlineByteBuddyMockMaker
import org.mockito.invocation.InvocationOnMock

import java.lang.reflect.Modifier
import java.util
import java.util.*

object ServerWatcher {

    class AppendListView[T](private val originalList: java.util.List[T], private val additionalList: java.util.List[T]) extends java.util.AbstractSequentialList[T] {

        override def listIterator(i: Int): util.ListIterator[T] = {
            val os = originalList.size
            if (i < os) originalList.listIterator(i)
            else additionalList.listIterator(i - os)
        }

        override def size: Int = originalList.size + additionalList.size
    }

}

class ServerWatcher {
    final val fakePlayers = new util.ArrayList[Player]
    private var origServer: Server = null

    @IgnoreForBinding
    @throws[Exception]
    def enableDisable(enable: Boolean): Unit = {
        val serverField = classOf[Bukkit].getDeclaredField("server")
        serverField.setAccessible(true)
        if (enable) {
            val serverClass = Bukkit.getServer.getClass
            val originalServer = serverField.get(null)
            DelegatingMockMaker.getInstance.setMockMaker(new InlineByteBuddyMockMaker)
            val settings = Mockito.withSettings.stubOnly.defaultAnswer((invocation: InvocationOnMock) => {
                def foo(invocation: InvocationOnMock): AnyRef = {
                    val method = invocation.getMethod
                    val pc = method.getParameterCount
                    var player = Option.empty[DiscordConnectedPlayer]
                    method.getName match {
                        case "getPlayer" =>
                            if (pc == 1 && (method.getParameterTypes()(0) == classOf[UUID]))
                                player = MCChatUtils.LoggedInPlayers.get(invocation.getArgument[UUID](0))
                        case "getPlayerExact" =>
                            if (pc == 1) {
                                val argument = invocation.getArgument(0)
                                player = MCChatUtils.LoggedInPlayers.values.find(_.getName.equalsIgnoreCase(argument))
                            }

                        /*case "getOnlinePlayers":
                                      if (playerList == null) {
                                        @SuppressWarnings("unchecked") var list = (List<Player>) method.invoke(origServer, invocation.getArguments());
                                        playerList = new AppendListView<>(list, fakePlayers);
                                      } - Your scientists were so preoccupied with whether or not they could, they didn’t stop to think if they should.
                                      return playerList;*/
                        case "createProfile" => //Paper's method, casts the player to a CraftPlayer
                            if (pc == 2) {
                                val uuid = invocation.getArgument(0)
                                val name = invocation.getArgument(1)
                                player = if (uuid != null) MCChatUtils.LoggedInPlayers.get(uuid) else Option.empty
                                if (player.isEmpty && name != null)
                                    player = MCChatUtils.LoggedInPlayers.values.find(_.getName.equalsIgnoreCase(name))
                                if (player.nonEmpty)
                                    return new CraftPlayerProfile(player.get.getUniqueId, player.get.getName)
                            }
                    }
                    if (player.nonEmpty) return player.get
                    method.invoke(origServer, invocation.getArguments)
                }

                foo(invocation)
            })
            //var mock = mockMaker.createMock(settings, MockHandlerFactory.createMockHandler(settings));
            //thread.setContextClassLoader(cl);
            val mock = Mockito.mock(serverClass, settings)
            for (field <- serverClass.getFields) { //Copy public fields, private fields aren't accessible directly anyways
                if (!Modifier.isFinal(field.getModifiers) && !Modifier.isStatic(field.getModifiers)) field.set(mock, field.get(originalServer))
            }
            serverField.set(null, mock)
            origServer = originalServer.asInstanceOf[Server]
        }
        else if (origServer != null) serverField.set(null, origServer)
    }
}