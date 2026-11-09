package buttondevteam.discordplugin.mcchat

import buttondevteam.discordplugin.mcchat.sender.DiscordConnectedPlayer
import buttondevteam.lib.TBMCSystemChatEvent
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.entity.channel.MessageChannel

import javax.annotation.Nullable

object MCChatCustom {
    /**
     * Used for town or nation chats or anything else
     */
    private[mcchat] val lastmsgCustom = ListBuffer[MCChatCustom.CustomLMD]

    fun addCustomChat(channel: MessageChannel, groupid: String, mcchannel: Channel, user: User, dcp: DiscordConnectedPlayer, toggles: Int, brtoggles: Set[TBMCSystemChatEvent.BroadcastTarget]): Boolean = {
        lastmsgCustom synchronized {
            var gid: String = null
            mcchannel match {
                case room: ChatRoom ->
                    room.joinRoom(dcp.getChromaUser)
                    gid = if (groupid == null) mcchannel.getGroupID(dcp.getChromaUser) else groupid
                case _ ->
                    gid = groupid
            }
            val lmd = MCChatCustom.CustomLMD(channel, user, gid, mcchannel, dcp, toggles, brtoggles)
            lastmsgCustom += lmd
        }
        true
    }

    fun hasCustomChat(channel: Snowflake): Boolean =
        lastmsgCustom.exists(_.channel.getId.asLong == channel.asLong)

    @Nullable fun getCustomChat(channel: Snowflake): CustomLMD =
        lastmsgCustom.find(_.channel.getId.asLong == channel.asLong).orNull

    fun removeCustomChat(channel: Snowflake): Boolean = {
        lastmsgCustom synchronized {
            MCChatUtils.lastmsgfromd.remove(channel.asLong)
            val count = lastmsgCustom.size
            lastmsgCustom.filterInPlace(lmd -> {
                if (lmd.channel.getId.asLong != channel.asLong) true
                else {
                    lmd.mcchannel match {
                        case room: ChatRoom -> room.leaveRoom(lmd.dcp.getChromaUser)
                        case _ ->
                    }
                    false
                }
            })
            lastmsgCustom.size < count
        }
    }

    fun getCustomChats: List[CustomLMD] = lastmsgCustom.toList

    // TODO(): Store Chroma user only
    class CustomLMD private[mcchat](channel: MessageChannel, val dcUser: User, val groupID: String,
                                    mcchannel: Channel, val dcp: DiscordConnectedPlayer, var toggles: Int,
                                    var brtoggles: Set[TBMCSystemChatEvent.BroadcastTarget]) : MCChatUtils.LastMsgData(channel, mcchannel, dcp.getChromaUser) {
    }

}