package buttondevteam.discordplugin.listeners;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCExceptionEvent;

public class ExceptionListener implements Listener {
	@EventHandler
	public void onException(TBMCExceptionEvent e) {
		SendException(e.getException(), e.getSourceMessage());
		e.setHandled();
	}

	private static void SendException(Throwable e, String sourcemessage) {
		try {
			StringBuilder sb = new StringBuilder();
			sb.append(sourcemessage).append("\n");
			sb.append("```").append("\n");
			String stackTrace = ExceptionUtils.getStackTrace(e);
			if (stackTrace.length() > 2000)
				stackTrace = stackTrace.substring(0, 2000);
			sb.append(stackTrace).append("\n");
			sb.append("```");
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.botchannel, sb.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
