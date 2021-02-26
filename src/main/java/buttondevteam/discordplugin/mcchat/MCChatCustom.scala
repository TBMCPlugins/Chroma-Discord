package buttondevteam.discordplugin.mcchat

import buttondevteam.core.component.channel.{Channel, ChatRoom}
import buttondevteam.discordplugin.DiscordConnectedPlayer
import buttondevteam.lib.TBMCSystemChatEvent
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import lombok.NonNull

import java.util
import java.util.Collections
import javax.annotation.Nullable

object MCChatCustom {
    /**
     * Used for town or nation chats or anything else
     */
    private[mcchat] val lastmsgCustom = new util.ArrayList[MCChatCustom.CustomLMD]

    def addCustomChat(channel: MessageChannel, groupid: String, mcchannel: Channel, user: User, dcp: DiscordConnectedPlayer, toggles: Int, brtoggles: util.Set[TBMCSystemChatEvent.BroadcastTarget]): Boolean = {
        lastmsgCustom synchronized {
            var gid: String = null
            mcchannel match {
                case room: ChatRoom =>
                    room.joinRoom(dcp)
                    gid = if (groupid == null) mcchannel.getGroupID(dcp) else groupid
                case _ =>
                    gid = groupid
            }
            val lmd = new MCChatCustom.CustomLMD(channel, user, gid, mcchannel, dcp, toggles, brtoggles)
            lastmsgCustom.add(lmd)
        }
        true
    }

    def hasCustomChat(channel: Snowflake): Boolean =
        lastmsgCustom.stream.anyMatch((lmd: MCChatCustom.CustomLMD) => lmd.channel.getId.asLong == channel.asLong)

    @Nullable def getCustomChat(channel: Snowflake): CustomLMD =
        lastmsgCustom.stream.filter((lmd: MCChatCustom.CustomLMD) => lmd.channel.getId.asLong == channel.asLong).findAny.orElse(null)

    def removeCustomChat(channel: Snowflake): Boolean = {
        lastmsgCustom synchronized MCChatUtils.lastmsgfromd.remove(channel.asLong)
        lastmsgCustom.removeIf((lmd: MCChatCustom.CustomLMD) => {
            def foo(lmd: MCChatCustom.CustomLMD): Boolean = {
                if (lmd.channel.getId.asLong != channel.asLong) return false
                lmd.mcchannel match {
                    case room: ChatRoom => room.leaveRoom(lmd.dcp)
                    case _ =>
                }
                true
            }

            foo(lmd)
        })
    }

    def getCustomChats: util.List[CustomLMD] = Collections.unmodifiableList(lastmsgCustom)

    class CustomLMD private(@NonNull channel: MessageChannel, @NonNull user: User, val groupID: String,
                            @NonNull val mcchannel: Channel, val dcp: DiscordConnectedPlayer, var toggles: Int,
                            var brtoggles: Set[TBMCSystemChatEvent.BroadcastTarget]) extends MCChatUtils.LastMsgData(channel, user) {
    }

}