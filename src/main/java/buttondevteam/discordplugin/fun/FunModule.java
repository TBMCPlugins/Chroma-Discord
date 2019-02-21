package buttondevteam.discordplugin.fun;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DPUtils;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import com.google.common.collect.Lists;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import sx.blah.discord.handle.impl.events.user.PresenceUpdateEvent;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IMessage;
import sx.blah.discord.handle.obj.IRole;
import sx.blah.discord.handle.obj.StatusType;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class FunModule extends Component<DiscordPlugin> implements Listener {
	private static FunModule mod;

	private static final String[] serverReadyStrings = new String[]{"In one week from now", // Ali
			"Between now and the heat-death of the universe.", // Ghostise
			"Soon™", "Ask again this time next month", // Ghostise
			"In about 3 seconds", // Nicolai
			"After we finish 8 plugins", // Ali
			"Tomorrow.", // Ali
			"After one tiiiny feature", // Ali
			"Next commit", // Ali
			"After we finish strangling Towny", // Ali
			"When we kill every *fucking* bug", // Ali
			"Once the server stops screaming.", // Ali
			"After HL3 comes out", // Ali
			"Next time you ask", // Ali
			"When will *you* be open?" // Ali
	};

	private ConfigData<Boolean> serverReady() {
		return getConfig().getData("serverReady", true);
	}

	private ConfigData<ArrayList<String>> serverReadyAnswers() {
		return getConfig().getData("serverReadyAnswers", () -> Lists.newArrayList(serverReadyStrings)); //TODO: Test
	}

	private static final String[] serverReadyQuestions = new String[]{"when will the server be open",
			"when will the server be ready", "when will the server be done", "when will the server be complete",
			"when will the server be finished", "when's the server ready", "when's the server open",
			"Vhen vill ze server be open?"};

	private static final Random serverReadyRandom = new Random();
	private static final ArrayList<Short> usableServerReadyStrings = new ArrayList<>(0);

	private void createUsableServerReadyStrings() {
		IntStream.range(0, serverReadyAnswers().get().size())
			.forEach(i -> FunModule.usableServerReadyStrings.add((short) i));
	}

	@Override
	protected void enable() {
		mod = this;
		registerListener(this);
	}

	@Override
	protected void disable() {
		lastlist = lastlistp = ListC = 0;
	}

	private static short lastlist = 0;
	private static short lastlistp = 0;

	private static short ListC = 0;

	public static boolean executeMemes(IMessage message) {
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
			message.reply("Stop it. You know the answer.");
			lastlist = 0;
			lastlistp = (short) Bukkit.getOnlinePlayers().size();
			return true; //Handled
		}
		lastlistp = (short) Bukkit.getOnlinePlayers().size(); //Didn't handle
		if (mod.serverReady().get()) {
			if (!TBMCCoreAPI.IsTestServer()
				&& Arrays.stream(serverReadyQuestions).anyMatch(msglowercased::contains)) {
				int next;
				if (usableServerReadyStrings.size() == 0)
					fm.createUsableServerReadyStrings();
				next = usableServerReadyStrings.remove(serverReadyRandom.nextInt(usableServerReadyStrings.size()));
				DiscordPlugin.sendMessageToChannel(message.getChannel(), serverReadyStrings[next]);
				return false; //Still process it as a command/mcchat if needed
			}
		}
		return false;
	}

	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		ListC = 0;
	}

	private ConfigData<IRole> fullHouseDevRole() {
		return getConfig().getDataPrimDef("fullHouseDevRole", "Developer", name -> {
			val list = DiscordPlugin.devServer.getRolesByName((String) name);
			return list.size() > 0 ? list.get(0) : null;
		}, IRole::getName);
	}


	private ConfigData<IChannel> fullHouseChannel() {
		return DPUtils.channelData(getConfig(), "fullHouseChannel", 219626707458457603L);
	}

	private static long lasttime = 0;

	public static void handleFullHouse(PresenceUpdateEvent event) {
		val fm = ComponentManager.getIfEnabled(FunModule.class);
		if (fm == null) return;
		val devrole = fm.fullHouseDevRole().get();
		if (devrole == null) return;
		if (event.getOldPresence().getStatus().equals(StatusType.OFFLINE)
			&& !event.getNewPresence().getStatus().equals(StatusType.OFFLINE)
			&& event.getUser().getRolesForGuild(DiscordPlugin.devServer).stream()
			.anyMatch(r -> r.getLongID() == devrole.getLongID())
			&& DiscordPlugin.devServer.getUsersByRole(devrole).stream()
			.noneMatch(u -> u.getPresence().getStatus().equals(StatusType.OFFLINE))
			&& lasttime + 10 < TimeUnit.NANOSECONDS.toHours(System.nanoTime())
			&& Calendar.getInstance().get(Calendar.DAY_OF_MONTH) % 5 == 0) {
			DiscordPlugin.sendMessageToChannel(mod.fullHouseChannel().get(), "Full house!",
				new EmbedBuilder()
					.withImage(
						"https://cdn.discordapp.com/attachments/249295547263877121/249687682618359808/poker-hand-full-house-aces-kings-playing-cards-15553791.png")
					.build());
			lasttime = TimeUnit.NANOSECONDS.toHours(System.nanoTime());
		}
	}
}
