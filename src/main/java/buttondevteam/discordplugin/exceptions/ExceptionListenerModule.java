package buttondevteam.discordplugin.exceptions;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.TBMCExceptionEvent;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.Role;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import reactor.core.publisher.Mono;

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
			getChannel().flatMap(channel -> {
				Mono<Role> coderRole;
				if (channel instanceof GuildChannel)
					coderRole = instance.pingRole(((GuildChannel) channel).getGuild()).get();
				else
					coderRole = Mono.empty();
				return coderRole.map(role -> TBMCCoreAPI.IsTestServer() ? new StringBuilder()
					: new StringBuilder(role.getMention()).append("\n"))
					.defaultIfEmpty(new StringBuilder())
					.flatMap(sb -> {
						sb.append(sourcemessage).append("\n");
						sb.append("```").append("\n");
						String stackTrace = Arrays.stream(ExceptionUtils.getStackTrace(e).split("\\n"))
							.filter(s -> !s.contains("\tat ") || s.contains("\tat buttondevteam."))
							.collect(Collectors.joining("\n"));
						if (sb.length() + stackTrace.length() >= 1980)
							stackTrace = stackTrace.substring(0, 1980 - sb.length());
						sb.append(stackTrace).append("\n");
						sb.append("```");
						return channel.createMessage(sb.toString());
					});
			}).subscribe();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static ExceptionListenerModule instance;

	public static Mono<MessageChannel> getChannel() {
		if (instance != null) return instance.channel().get();
		return Mono.empty();
	}

	private ReadOnlyConfigData<Mono<MessageChannel>> channel() {
		return DPUtils.channelData(getConfig(), "channel");
	}

	private ConfigData<Mono<Role>> pingRole(Mono<Guild> guild) {
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
