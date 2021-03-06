package buttondevteam.discordplugin.mcchat

import buttondevteam.core.component.channel.{Channel, ChatRoom}
import buttondevteam.discordplugin.DiscordConnectedPlayer
import buttondevteam.lib.TBMCSystemChatEvent
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel
import lombok.NonNull

import javax.annotation.Nullable
import scala.collection.mutable.ListBuffer

object MCChatCustom {
    /**
     * Used for town or nation chats or anything else
     */
    private[mcchat] val lastmsgCustom = new ListBuffer[MCChatCustom.CustomLMD]

    def addCustomChat(channel: MessageChannel, groupid: String, mcchannel: Channel, user: User, dcp: DiscordConnectedPlayer, toggles: Int, brtoggles: Set[TBMCSystemChatEvent.BroadcastTarget]): Boolean = {
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
            lastmsgCustom += lmd
        }
        true
    }

    def hasCustomChat(channel: Snowflake): Boolean =
        lastmsgCustom.exists(_.channel.getId.asLong == channel.asLong)

    @Nullable def getCustomChat(channel: Snowflake): CustomLMD =
        lastmsgCustom.find(_.channel.getId.asLong == channel.asLong).orNull

    def removeCustomChat(channel: Snowflake): Unit = {
        lastmsgCustom synchronized {
            MCChatUtils.lastmsgfromd.remove(channel.asLong)
            lastmsgCustom.filterInPlace(lmd => {
                if (lmd.channel.getId.asLong != channel.asLong) return true
                lmd.mcchannel match {
                    case room: ChatRoom => room.leaveRoom(lmd.dcp)
                    case _ =>
                }
                false
            })
        }
    }

    def getCustomChats: List[CustomLMD] = lastmsgCustom.toList

    class CustomLMD private[mcchat](@NonNull channel: MessageChannel, @NonNull user: User, val groupID: String,
                                    @NonNull mcchannel: Channel, val dcp: DiscordConnectedPlayer, var toggles: Int,
                                    var brtoggles: Set[TBMCSystemChatEvent.BroadcastTarget]) extends MCChatUtils.LastMsgData(channel, user, mcchannel) {
    }

}