package buttondevteam.discordplugin;

public enum ChannelconBroadcast {
	JOINLEAVE,
	AFK,
	RESTART, //TODO
	DEATH,
	BROADCAST;

	public final int flag;

	ChannelconBroadcast() {
		this.flag = 1 << this.ordinal();
	}
}
