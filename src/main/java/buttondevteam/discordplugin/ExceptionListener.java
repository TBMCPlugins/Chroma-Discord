package buttondevteam.discordplugin;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.lib.TBMCExceptionEvent;

public class ExceptionListener implements Listener {
	@EventHandler
	public void onException(TBMCExceptionEvent e) {
		SendException(e.getException(), e.getSourceMessage());
	}

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
