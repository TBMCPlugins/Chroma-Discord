package buttondevteam.discordplugin.fun;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import buttondevteam.lib.architecture.ReadOnlyConfigData;
import com.google.common.collect.Lists;
import discord4j.core.event.domain.PresenceUpdateEvent;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.Role;
import discord4j.core.object.entity.channel.GuildChannel;
import discord4j.core.object.entity.channel.MessageChannel;
import discord4j.core.object.presence.Status;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * All kinds of random things.
 * The YEEHAW event uses an emoji named :YEEHAW: if available
 */
public class FunModule extends Component<DiscordPlugin> implements Listener {
	private static final String[] serverReadyStrings = new String[]{"in one week from now", // Ali
		"between now and the heat-death of the universe.", // Ghostise
		"soon™", "ask again this time next month", // Ghostise
		"in about 3 seconds", // Nicolai
		"after we finish 8 plugins", // Ali
		"tomorrow.", // Ali
		"after one tiiiny feature", // Ali
		"next commit", // Ali
		"after we finish strangling Towny", // Ali
		"when we kill every *fucking* bug", // Ali
		"once the server stops screaming.", // Ali
		"after HL3 comes out", // Ali
		"next time you ask", // Ali
		"when will *you* be open?" // Ali
	};

	/**
	 * Questions that the bot will choose a random answer to give to.
	 */
	private ConfigData<String[]> serverReady() {
		return getConfig().getData("serverReady", () -> new String[]{"when will the server be open",
			"when will the server be ready", "when will the server be done", "when will the server be complete",
			"when will the server be finished", "when's the server ready", "when's the server open",
			"vhen vill ze server be open?"});
	}

	/**
	 * Answers for a recognized question. Selected randomly.
	 */
	private ConfigData<ArrayList<String>> serverReadyAnswers() {
		return getConfig().getData("serverReadyAnswers", () -> Lists.newArrayList(serverReadyStrings));
	}

	private static final Random serverReadyRandom = new Random();
	private static final ArrayList<Short> usableServerReadyStrings = new ArrayList<>(0);

	private void createUsableServerReadyStrings() {
		IntStream.range(0, serverReadyAnswers().get().size())
			.forEach(i -> FunModule.usableServerReadyStrings.add((short) i));
	}

	@Override
	protected void enable() {
		registerListener(this);
	}

	@Override
	protected void disable() {
		lastlist = lastlistp = ListC = 0;
	}

	private static short lastlist = 0;
	private static short lastlistp = 0;

	private static short ListC = 0;

	public static boolean executeMemes(Message message) {
		val fm = ComponentManager.getIfEnabled(FunModule.class);
		if (fm == null) return false;
		String msglowercased = message.getContent().toLowerCase();
		lastlist++;
		if (lastlist > 5) {
			ListC = 0;
			lastlist = 0;
		}
		if (msglowercased.equals("list") && Bukkit.getOnlinePlayers().size() == lastlistp && ListC++ > 2) // Lowered already
		{
			DPUtils.reply(message, Mono.empty(), "stop it. You know the answer.").subscribe();
			lastlist = 0;
			lastlistp = (short) Bukkit.getOnlinePlayers().size();
			return true; //Handled
		}
		lastlistp = (short) Bukkit.getOnlinePlayers().size(); //Didn't handle
		if (!TBMCCoreAPI.IsTestServer()
			&& Arrays.stream(fm.serverReady().get()).anyMatch(msglowercased::contains)) {
			int next;
			if (usableServerReadyStrings.size() == 0)
				fm.createUsableServerReadyStrings();
			next = usableServerReadyStrings.remove(serverReadyRandom.nextInt(usableServerReadyStrings.size()));
			DPUtils.reply(message, Mono.empty(), fm.serverReadyAnswers().get().get(next)).subscribe();
			return false; //Still process it as a command/mcchat if needed
		}
		return false;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		ListC = 0;
	}

	/**
	 * If all of the people who have this role are online, the bot will post a full house.
	 */
	private ConfigData<Mono<Role>> fullHouseDevRole(Mono<Guild> guild) {
		return DPUtils.roleData(getConfig(), "fullHouseDevRole", "Developer", guild);
	}


	/**
	 * The channel to post the full house to.
	 */
	private ReadOnlyConfigData<Mono<MessageChannel>> fullHouseChannel() {
		return DPUtils.channelData(getConfig(), "fullHouseChannel");
	}

	private static long lasttime = 0;

	public static void handleFullHouse(PresenceUpdateEvent event) {
		val fm = ComponentManager.getIfEnabled(FunModule.class);
		if (fm == null) return;
		if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 5 != 0) return;
		fm.fullHouseChannel().get()
			.filter(ch -> ch instanceof GuildChannel)
			.flatMap(channel -> fm.fullHouseDevRole(((GuildChannel) channel).getGuild()).get()
				.filter(role -> event.getOld().map(p -> p.getStatus().equals(Status.OFFLINE)).orElse(false))
				.filter(role -> !event.getCurrent().getStatus().equals(Status.OFFLINE))
				.filterWhen(devrole -> event.getMember().flatMap(m -> m.getRoles()
					.any(r -> r.getId().asLong() == devrole.getId().asLong())))
				.filterWhen(devrole ->
					event.getGuild().flatMapMany(g -> g.getMembers().filter(m -> m.getRoleIds().stream().anyMatch(s -> s.equals(devrole.getId()))))
						.flatMap(Member::getPresence).all(pr -> !pr.getStatus().equals(Status.OFFLINE)))
				.filter(devrole -> lasttime + 10 < TimeUnit.NANOSECONDS.toHours(System.nanoTime())) //This should stay so it checks this last
				.flatMap(devrole -> {
					lasttime = TimeUnit.NANOSECONDS.toHours(System.nanoTime());
					return channel.createMessage(mcs -> mcs.setContent("Full house!").setEmbed(ecs ->
						ecs.setImage(
							"https://cdn.discordapp.com/attachments/249295547263877121/249687682618359808/poker-hand-full-house-aces-kings-playing-cards-15553791.png")
					));
				})).subscribe();
	}
}
