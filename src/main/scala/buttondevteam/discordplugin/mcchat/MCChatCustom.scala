package buttondevteam.discordplugin.mcchat

import buttondevteam.core.component.channel.{Channel, ChatRoom}
import buttondevteam.discordplugin.mcchat.sender.DiscordConnectedPlayer
import buttondevteam.lib.TBMCSystemChatEvent
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel

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
                    room.joinRoom(dcp.getChromaUser)
                    gid = if (groupid == null) mcchannel.getGroupID(dcp.getChromaUser) else groupid
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

    def removeCustomChat(channel: Snowflake): Boolean = {
        lastmsgCustom synchronized {
            MCChatUtils.lastmsgfromd.remove(channel.asLong)
            val count = lastmsgCustom.size
            lastmsgCustom.filterInPlace(lmd => {
                if (lmd.channel.getId.asLong != channel.asLong) true
                else {
                    lmd.mcchannel match {
                        case room: ChatRoom => room.leaveRoom(lmd.dcp.getChromaUser)
                        case _ =>
                    }
                    false
                }
            })
            lastmsgCustom.size < count
        }
    }

    def getCustomChats: List[CustomLMD] = lastmsgCustom.toList

    // TODO: Store Chroma user only
    class CustomLMD private[mcchat](channel: MessageChannel, user: User, val groupID: String,
                                    mcchannel: Channel, val dcp: DiscordConnectedPlayer, var toggles: Int,
                                    var brtoggles: Set[TBMCSystemChatEvent.BroadcastTarget]) extends MCChatUtils.LastMsgData(channel, user, mcchannel) {
    }

}