package buttondevteam.discordplugin.broadcaster

import buttondevteam.discordplugin.mcchat.MCChatUtils
import buttondevteam.discordplugin.playerfaker.DelegatingMockMaker
import buttondevteam.lib.TBMCCoreAPI
import org.bukkit.Bukkit
import org.mockito.Mockito
import org.mockito.internal.creation.bytebuddy.SubclassByteBuddyMockMaker
import org.mockito.invocation.InvocationOnMock
import org.mockito.stubbing.Answer

import java.lang.invoke.{MethodHandle, MethodHandles}
import java.lang.reflect.{Constructor, Method, Modifier}
import java.util.UUID

object PlayerListWatcher {
    private var plist: AnyRef = null
    private var mock: AnyRef = null
    private var fHandle: MethodHandle = null //Handle for PlayerList.f(EntityPlayer) - Only needed for 1.16
    @throws[Exception]
    private[broadcaster] def hookUpDown(up: Boolean, module: GeneralEventBroadcasterModule): Boolean = {
        val csc = Bukkit.getServer.getClass
        val conf = csc.getDeclaredField("console")
        conf.setAccessible(true)
        val server = conf.get(Bukkit.getServer)
        val nms = server.getClass.getPackage.getName
        val dplc = Class.forName(nms + ".DedicatedPlayerList")
        val currentPL = server.getClass.getMethod("getPlayerList").invoke(server)
        if (up) {
            if (currentPL eq mock) {
                module.logWarn("Player list already mocked!")
                return false
            }
            DelegatingMockMaker.getInstance.setMockMaker(new SubclassByteBuddyMockMaker)
            val icbcl = Class.forName(nms + ".IChatBaseComponent")
            var sendMessageTemp: Method = null
            try sendMessageTemp = server.getClass.getMethod("sendMessage", icbcl, classOf[UUID])
            catch {
                case e: NoSuchMethodException =>
                    sendMessageTemp = server.getClass.getMethod("sendMessage", icbcl)
            }
            val sendMessageMethod = sendMessageTemp
            val cmtcl = Class.forName(nms + ".ChatMessageType")
            val systemType = cmtcl.getDeclaredField("SYSTEM").get(null)
            val chatType = cmtcl.getDeclaredField("CHAT").get(null)
            val obc = csc.getPackage.getName
            val ccmcl = Class.forName(obc + ".util.CraftChatMessage")
            val fixComponent = ccmcl.getMethod("fixComponent", icbcl)
            val ppoc = Class.forName(nms + ".PacketPlayOutChat")
            var ppocCTemp: Constructor[_] = null
            try ppocCTemp = ppoc.getConstructor(icbcl, cmtcl, classOf[UUID])
            catch {
                case _: Exception =>
                    ppocCTemp = ppoc.getConstructor(icbcl, cmtcl)
            }
            val ppocC = ppocCTemp
            val sendAllMethod = dplc.getMethod("sendAll", Class.forName(nms + ".Packet"))
            var tpt: Method = null
            try tpt = icbcl.getMethod("toPlainText")
            catch {
                case _: NoSuchMethodException =>
                    tpt = icbcl.getMethod("getString")
            }
            val toPlainText = tpt
            val sysb = Class.forName(nms + ".SystemUtils").getField("b")
            //Find the original method without overrides
            var lookupConstructor: Constructor[MethodHandles.Lookup] = null
            if (nms.contains("1_16")) {
                lookupConstructor = classOf[MethodHandles.Lookup].getDeclaredConstructor(classOf[Class[_]])
                lookupConstructor.setAccessible(true) //Create lookup with a given class instead of caller
            }
            else lookupConstructor = null
            mock = Mockito.mock(dplc, Mockito.withSettings.defaultAnswer(new Answer[AnyRef]() { // Cannot call super constructor
                @throws[Throwable]
                override def answer(invocation: InvocationOnMock): AnyRef = {
                    val method = invocation.getMethod
                    if (!(method.getName == "sendMessage")) {
                        if (method.getName == "sendAll") {
                            sendAll(invocation.getArgument(0))
                            return null
                        }
                        //In 1.16 it passes a reference to the player list to advancement data for each player
                        if (nms.contains("1_16") && method.getName == "f" && method.getParameterCount > 0 && method.getParameterTypes()(0).getSimpleName == "EntityPlayer") {
                            method.setAccessible(true)
                            if (fHandle == null) {
                                assert(lookupConstructor != null)
                                val lookup = lookupConstructor.newInstance(mock.getClass)
                                fHandle = lookup.unreflectSpecial(method, mock.getClass) //Special: super.method()
                            }
                            return fHandle.invoke(mock, invocation.getArgument(0)) //Invoke with our instance, so it passes that to advancement data, we have the fields as well
                        }
                        return method.invoke(plist, invocation.getArguments)
                    }
                    val args = invocation.getArguments
                    val params = method.getParameterTypes
                    if (params.isEmpty) {
                        TBMCCoreAPI.SendException("Found a strange method", new Exception("Found a sendMessage() method without arguments."), module)
                        return null
                    }
                    if (params(0).getSimpleName == "IChatBaseComponent[]") for (arg <- args(0).asInstanceOf[Array[AnyRef]]) {
                        sendMessage(arg, system = true)
                    }
                    else if (params(0).getSimpleName == "IChatBaseComponent") if (params.length > 1 && params(1).getSimpleName.equalsIgnoreCase("boolean")) sendMessage(args(0), args(1).asInstanceOf[Boolean])
                    else sendMessage(args(0), system = true)
                    else TBMCCoreAPI.SendException("Found a method with interesting params", new Exception("Found a sendMessage(" + params(0).getSimpleName + ") method"), module)
                    null
                }

                private

                def sendMessage(chatComponent: Any, system: Boolean) = try { //Converted to use reflection
                    if (sendMessageMethod.getParameterCount == 2) sendMessageMethod.invoke(server, chatComponent, sysb.get(null))
                    else sendMessageMethod.invoke(server, chatComponent)
                    val chatmessagetype = if (system) systemType
                    else chatType
                    // CraftBukkit start - we run this through our processor first so we can get web links etc
                    val comp = fixComponent.invoke(null, chatComponent)
                    val packet = if (ppocC.getParameterCount == 3) ppocC.newInstance(comp, chatmessagetype, sysb.get(null))
                    else ppocC.newInstance(comp, chatmessagetype)
                    this.sendAll(packet)
                } catch {
                    case e: Exception =>
                        TBMCCoreAPI.SendException("An error occurred while passing a vanilla message through the player list", e, module)
                }

                private

                def sendAll(packet: Any) = try { // Some messages get sent by directly constructing a packet
                    sendAllMethod.invoke(plist, packet)
                    if (packet.getClass eq ppoc) {
                        val msgf = ppoc.getDeclaredField("a")
                        msgf.setAccessible(true)
                        MCChatUtils.forPublicPrivateChat(MCChatUtils.send(toPlainText.invoke(msgf.get(packet)).asInstanceOf[String])).subscribe()
                    }
                } catch {
                    case e: Exception =>
                        TBMCCoreAPI.SendException("Failed to broadcast message sent to all players - hacking failed.", e, module)
                }
            }).stubOnly).asInstanceOf
            plist = currentPL
            var plc = dplc
            while ( {
                plc != null
            }) { //Set all fields
                for (f <- plc.getDeclaredFields) {
                    f.setAccessible(true)
                    val modf = f.getClass.getDeclaredField("modifiers")
                    modf.setAccessible(true)
                    modf.set(f, f.getModifiers & ~Modifier.FINAL)
                    f.set(mock, f.get(plist))
                }
                plc = plc.getSuperclass
            }
        }
        try server.getClass.getMethod("a", dplc).invoke(server, if (up) mock
        else plist)
        catch {
            case e: NoSuchMethodException =>
                server.getClass.getMethod("a", Class.forName(server.getClass.getPackage.getName + ".PlayerList")).invoke(server, if (up) mock
                else plist)
        }
        val pllf = csc.getDeclaredField("playerList")
        pllf.setAccessible(true)
        pllf.set(Bukkit.getServer, if (up) mock
        else plist)
        true
    }
}