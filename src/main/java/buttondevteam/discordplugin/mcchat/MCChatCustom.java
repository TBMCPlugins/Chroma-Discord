package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.component.channel.Channel;
import buttondevteam.core.component.channel.ChatRoom;
import buttondevteam.discordplugin.DiscordConnectedPlayer;
import buttondevteam.lib.TBMCSystemChatEvent;
import discord4j.common.util.Snowflake;
import discord4j.core.object.entity.User;
import discord4j.core.object.entity.channel.MessageChannel;
import lombok.NonNull;
import lombok.val;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class MCChatCustom {
	/**
	 * Used for town or nation chats or anything else
	 */
	static ArrayList<CustomLMD> lastmsgCustom = new ArrayList<>();

	public static void addCustomChat(MessageChannel channel, String groupid, Channel mcchannel, User user, DiscordConnectedPlayer dcp, int toggles, Set<TBMCSystemChatEvent.BroadcastTarget> brtoggles) {
		if (mcchannel instanceof ChatRoom) {
			((ChatRoom) mcchannel).joinRoom(dcp);
			if (groupid == null) groupid = mcchannel.getGroupID(dcp);
		}
		val lmd = new CustomLMD(channel, user, groupid, mcchannel, dcp, toggles, brtoggles);
		lastmsgCustom.add(lmd);
	}

	public static boolean hasCustomChat(Snowflake channel) {
		return lastmsgCustom.stream().anyMatch(lmd -> lmd.channel.getId().asLong() == channel.asLong());
	}

	@Nullable
	public static CustomLMD getCustomChat(Snowflake channel) {
		return lastmsgCustom.stream().filter(lmd -> lmd.channel.getId().asLong() == channel.asLong()).findAny().orElse(null);
	}

	public static boolean removeCustomChat(Snowflake channel) {
		MCChatUtils.lastmsgfromd.remove(channel.asLong());
		return lastmsgCustom.removeIf(lmd -> {
			if (lmd.channel.getId().asLong() != channel.asLong())
				return false;
			if (lmd.mcchannel instanceof ChatRoom)
				((ChatRoom) lmd.mcchannel).leaveRoom(lmd.dcp);
			return true;
		});
	}

	public static List<CustomLMD> getCustomChats() {
		return Collections.unmodifiableList(lastmsgCustom);
	}

	public static class CustomLMD extends MCChatUtils.LastMsgData {
		public final String groupID;
		public final DiscordConnectedPlayer dcp;
		public int toggles;
		public Set<TBMCSystemChatEvent.BroadcastTarget> brtoggles;

		private CustomLMD(@NonNull MessageChannel channel, @NonNull User user,
		                  @NonNull String groupid, @NonNull Channel mcchannel, @NonNull DiscordConnectedPlayer dcp, int toggles, Set<TBMCSystemChatEvent.BroadcastTarget> brtoggles) {
			super(channel, user);
			groupID = groupid;
			this.mcchannel = mcchannel;
			this.dcp = dcp;
			this.toggles = toggles;
			this.brtoggles = brtoggles;
		}
	}
}
