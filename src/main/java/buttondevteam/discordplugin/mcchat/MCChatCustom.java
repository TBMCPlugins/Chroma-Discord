package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.component.channel.Channel;
import buttondevteam.discordplugin.DiscordConnectedPlayer;
import lombok.NonNull;
import lombok.val;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MCChatCustom {
	/**
	 * Used for town or nation chats or anything else
	 */
	static ArrayList<CustomLMD> lastmsgCustom = new ArrayList<>();

	public static void addCustomChat(IChannel channel, String groupid, Channel mcchannel, IUser user, DiscordConnectedPlayer dcp, int toggles) {
		val lmd = new CustomLMD(channel, user, groupid, mcchannel, dcp, toggles);
		lastmsgCustom.add(lmd);
	}

	public static boolean hasCustomChat(IChannel channel) {
		return lastmsgCustom.stream().anyMatch(lmd -> lmd.channel.getLongID() == channel.getLongID());
	}

	@Nullable
	public static CustomLMD getCustomChat(IChannel channel) {
		return lastmsgCustom.stream().filter(lmd -> lmd.channel.getLongID() == channel.getLongID()).findAny().orElse(null);
	}

	public static boolean removeCustomChat(IChannel channel) {
		MCChatUtils.lastmsgfromd.remove(channel.getLongID());
		return lastmsgCustom.removeIf(lmd -> lmd.channel.getLongID() == channel.getLongID());
	}

	public static List<CustomLMD> getCustomChats() {
		return Collections.unmodifiableList(lastmsgCustom);
	}

	public static class CustomLMD extends MCChatUtils.LastMsgData {
		public final String groupID;
		public final Channel mcchannel;
		public final DiscordConnectedPlayer dcp;
		public int toggles;

		private CustomLMD(@NonNull IChannel channel, @NonNull IUser user,
		                  @NonNull String groupid, @NonNull Channel mcchannel, @NonNull DiscordConnectedPlayer dcp, int toggles) {
			super(channel, user);
			groupID = groupid;
			this.mcchannel = mcchannel;
			this.dcp = dcp;
			this.toggles = toggles;
		}
	}
}
