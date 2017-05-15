package buttondevteam.discordplugin.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCExceptionEvent;
import sx.blah.discord.handle.obj.IRole;

public class ExceptionListener implements Listener {
	private List<Throwable> lastthrown = new ArrayList<>();
	private List<String> lastsourcemsg = new ArrayList<>();

	@EventHandler
	public void onException(TBMCExceptionEvent e) {
		if (DiscordPlugin.SafeMode)
			return;
		if (lastthrown.stream()
				.anyMatch(ex -> Arrays.equals(e.getException().getStackTrace(), ex.getStackTrace())
						&& e.getException().getMessage().equals(ex.getMessage()))
				&& lastsourcemsg.contains(e.getSourceMessage()))
			return;
		SendException(e.getException(), e.getSourceMessage());
		if (lastthrown.size() >= 10)
			lastthrown.remove(0);
		if (lastsourcemsg.size() >= 10)
			lastsourcemsg.remove(0);
		lastthrown.add(e.getException());
		lastsourcemsg.add(e.getSourceMessage());
		e.setHandled();
	}

	private static IRole coderRole;

	private static void SendException(Throwable e, String sourcemessage) {
		try {
			if (coderRole == null)
				coderRole = DiscordPlugin.devServer.getRolesByName("Coder").get(0);
			StringBuilder sb = TBMCCoreAPI.IsTestServer() ? new StringBuilder()
					: new StringBuilder(coderRole.mention()).append("\n");
			sb.append(sourcemessage).append("\n");
			sb.append("```").append("\n");
			String stackTrace = Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n"))
					.filter(s -> !(s.contains("\tat ") && ( //
					s.contains("java.util") //
							|| s.contains("java.lang") //
							|| s.contains("net.minecraft.server") //
							|| s.contains("sun.reflect") //
							|| s.contains("org.bukkit") //
					))).collect(Collectors.joining("\n"));
			if (stackTrace.length() > 1800)
				stackTrace = stackTrace.substring(0, 1800);
			sb.append(stackTrace).append("\n");
			sb.append("```");
			DiscordPlugin.sendMessageToChannel(DiscordPlugin.botroomchannel, sb.toString());
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
