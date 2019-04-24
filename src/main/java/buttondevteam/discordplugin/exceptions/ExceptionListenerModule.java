package buttondevteam.discordplugin.exceptions;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCExceptionEvent;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionListenerModule extends Component<DiscordPlugin> implements Listener {
    private List<Throwable> lastthrown = new ArrayList<>();
    private List<String> lastsourcemsg = new ArrayList<>();

    @EventHandler
    public void onException(TBMCExceptionEvent e) {
	    if (DiscordPlugin.SafeMode || !ComponentManager.isEnabled(getClass()))
            return;
        if (lastthrown.stream()
                .anyMatch(ex -> Arrays.equals(e.getException().getStackTrace(), ex.getStackTrace())
                        && (e.getException().getMessage() == null ? ex.getMessage() == null
                        : e.getException().getMessage().equals(ex.getMessage()))) // e.Exception.Message==ex.Message
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

    private static void SendException(Throwable e, String sourcemessage) {
		if (instance == null) return;
        try {
			MessageChannel channel = getChannel();
	        assert channel != null;
			Role coderRole = instance.pingRole(((GuildChannel) channel).getGuild().block()).get();
            StringBuilder sb = TBMCCoreAPI.IsTestServer() ? new StringBuilder()
				: new StringBuilder(coderRole == null ? "" : coderRole.getMention()).append("\n");
            sb.append(sourcemessage).append("\n");
            sb.append("```").append("\n");
            String stackTrace = Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n"))
                    .filter(s -> !s.contains("\tat ") || s.contains("\tat buttondevteam."))
                    .collect(Collectors.joining("\n"));
			if (sb.length() + stackTrace.length() >= 2000)
				stackTrace = stackTrace.substring(0, 1999 - sb.length());
            sb.append(stackTrace).append("\n");
            sb.append("```");
			channel.createMessage(sb.toString()).subscribe();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

	private static ExceptionListenerModule instance;

	public static MessageChannel getChannel() {
		if (instance != null) return instance.channel().get();
		return null;
	}

	private ConfigData<MessageChannel> channel() {
		return DPUtils.channelData(getConfig(), "channel", 239519012529111040L);
	}

	private ConfigData<Role> pingRole(Guild guild) {
		return DPUtils.roleData(getConfig(), "pingRole", "Coder", guild);
	}

	@Override
	protected void enable() {
		if (DPUtils.disableIfConfigError(this, channel())) return;
		instance = this;
		Bukkit.getPluginManager().registerEvents(new ExceptionListenerModule(), getPlugin());
		TBMCCoreAPI.RegisterEventsForExceptions(new DebugMessageListener(), getPlugin());
	}

	@Override
	protected void disable() {
		instance = null;
	}
}
