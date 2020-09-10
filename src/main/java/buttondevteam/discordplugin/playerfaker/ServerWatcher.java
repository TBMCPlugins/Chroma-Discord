package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.mcchat.MCChatUtils;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.mockito.Mockito;

import java.util.*;

public class ServerWatcher {
	private List<Player> playerList;
	private final List<Player> fakePlayers = new ArrayList<>();
	private Server origServer;

	public void enableDisable(boolean enable) throws Exception {
		var serverField = Bukkit.class.getDeclaredField("server");
		serverField.setAccessible(true);
		if (enable) {
			var serverClass = Bukkit.getServer().getClass();
			var mock = Mockito.mock(serverClass, Mockito.withSettings()
				.stubOnly().defaultAnswer(invocation -> {
					var method = invocation.getMethod();
					int pc = method.getParameterCount();
					Player player = null;
					switch (method.getName()) {
						case "getPlayer":
							if (pc == 1 && method.getParameterTypes()[0] == UUID.class)
								player = MCChatUtils.LoggedInPlayers.get(invocation.<UUID>getArgument(0));
							break;
						case "getPlayerExact":
							if (pc == 1) {
								final String argument = invocation.getArgument(0);
								player = MCChatUtils.LoggedInPlayers.values().stream()
									.filter(dcp -> dcp.getName().equalsIgnoreCase(argument)).findAny().orElse(null);
							}
							break;
						case "getOnlinePlayers":
							if (playerList == null) {
								@SuppressWarnings("unchecked") var list = (List<Player>) invocation.callRealMethod();
								playerList = new AppendListView<>(list, fakePlayers);
							}
							return playerList;
					}
					if (player != null)
						return player;
					return invocation.callRealMethod();
				}));
			var originalServer = serverField.get(null);
			for (var field : serverClass.getFields()) //Copy public fields, private fields aren't accessible directly anyways
				field.set(mock, field.get(originalServer));
			serverField.set(null, mock);
			origServer = (Server) originalServer;
		} else if (origServer != null)
			serverField.set(null, origServer);
	}

	@RequiredArgsConstructor
	public static class AppendListView<T> extends AbstractSequentialList<T> {
		private final List<T> originalList;
		private final List<T> additionalList;

		@Override
		public ListIterator<T> listIterator(int i) {
			int os = originalList.size();
			return i < os ? originalList.listIterator(i) : additionalList.listIterator(i - os);
		}

		@Override
		public int size() {
			return originalList.size() + additionalList.size();
		}
	}
}
