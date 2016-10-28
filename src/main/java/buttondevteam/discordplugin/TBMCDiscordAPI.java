package buttondevteam.discordplugin;

import org.apache.commons.lang.exception.ExceptionUtils;

public final class TBMCDiscordAPI {
	public static void SendException(Throwable e, String sourcemessage) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(sourcemessage).append("\n");
			sb.append("```").append("\n");
			sb.append(ExceptionUtils.getStackTrace(e)).append("\n");
			sb.append("```");
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.issuechannel, sb.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
