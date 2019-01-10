package buttondevteam.discordplugin.fun;

import buttondevteam.core.ComponentManager;
import buttondevteam.discordplugin.DiscordPlugin;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.architecture.Component;
import buttondevteam.lib.architecture.ConfigData;
import sx.blah.discord.handle.obj.IMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class FunModule extends Component {
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

	private ConfigData<List<String>> serverReadyAnswers() {
		return getConfig().getData("serverReadyAnswers", Arrays.asList(serverReadyStrings),
				data -> (List<String>) data, data -> data); //TODO: Test
	}

	private static final String[] serverReadyQuestions = new String[]{"when will the server be open",
			"when will the server be ready", "when will the server be done", "when will the server be complete",
			"when will the server be finished", "when's the server ready", "when's the server open",
			"Vhen vill ze server be open?"};

	private static final Random serverReadyRandom = new Random();
	private static final ArrayList<Short> usableServerReadyStrings = new ArrayList<Short>(serverReadyStrings.length) {
		private static final long serialVersionUID = 2213771460909848770L;

		{
			createUsableServerReadyStrings(this);
		}
	};

	private static void createUsableServerReadyStrings(ArrayList<Short> list) {
		for (short i = 0; i < serverReadyStrings.length; i++)
			list.add(i);
	}

	@Override
	protected void enable() {
		mod = this;
	}

	@Override
	protected void disable() {
	}

	public static boolean executeMemes(IMessage message) {
		if (!ComponentManager.isEnabled(FunModule.class)) return false;
		if (mod.serverReady().get()) {
			if (!TBMCCoreAPI.IsTestServer()
					&& Arrays.stream(serverReadyQuestions).anyMatch(s -> message.getContent().toLowerCase().contains(s))) {
				int next;
				if (usableServerReadyStrings.size() == 0)
					createUsableServerReadyStrings(usableServerReadyStrings);
				next = usableServerReadyStrings.remove(serverReadyRandom.nextInt(usableServerReadyStrings.size()));
				DiscordPlugin.sendMessageToChannel(message.getChannel(), serverReadyStrings[next]);
				return true;
			}
		}
		return false;
	}
}
