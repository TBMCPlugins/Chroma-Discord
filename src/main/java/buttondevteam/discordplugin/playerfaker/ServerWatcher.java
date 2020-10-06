package buttondevteam.discordplugin.playerfaker;

import buttondevteam.discordplugin.mcchat.MCChatUtils;
import buttondevteam.lib.TBMCCoreAPI;
import com.destroystokyo.paper.profile.CraftPlayerProfile;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.IgnoreForBinding;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.objenesis.ObjenesisStd;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.Callable;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class ServerWatcher {
	private List<Player> playerList;
	private final List<Player> fakePlayers = new ArrayList<>();
	private Server origServer;

	@IgnoreForBinding
	public void enableDisable(boolean enable) throws Exception {
		var serverField = Bukkit.class.getDeclaredField("server");
		serverField.setAccessible(true);
		if (enable) {
			var serverClass = Bukkit.getServer().getClass();
			//var mockMaker = new InlineByteBuddyMockMaker();
			//System.setProperty("net.bytebuddy.experimental", "true");
			/*try {
				var resources = cl.getResources("mockito-extensions/" + MockMaker.class.getName());
				System.out.println("Found resources: " + resources);
				Iterables.toIterable(resources).forEach(resource -> System.out.println("Resource: " + resource));
			} catch (IOException e) {
				throw new IllegalStateException("Failed to load " + MockMaker.class, e);
			}*/
			ByteBuddyAgent.install();
			var originalServer = serverField.get(null);
			var impl = MethodDelegation.to(this);
			var names = Arrays.stream(ServerWatcher.class.getMethods()).map(Method::getName).toArray(String[]::new);
			var utype = new ByteBuddy() //InlineBytecodeGenerator
				.with(TypeValidation.DISABLED)
				.with(Implementation.Context.Disabled.Factory.INSTANCE)
				.with(MethodGraph.Compiler.ForDeclaredMethods.INSTANCE)
				.ignore(isSynthetic().and(not(isConstructor())).or(isDefaultFinalizer()))
				.redefine(serverClass)
				.method(target -> !target.isStatic()).intercept(MethodCall.invokeSelf().on(originalServer).withAllArguments())
				.method(target -> Arrays.stream(names).anyMatch(target.getActualName()::equalsIgnoreCase)).intercept(impl).make();
			var ltype = utype.load(serverClass.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
			var type = ltype.getLoaded();
			var mock = new ObjenesisStd().newInstance(type);
			for (var field : serverClass.getFields()) //Copy public fields, private fields aren't accessible directly anyways
				if (!Modifier.isFinal(field.getModifiers()) && !Modifier.isStatic(field.getModifiers()))
					field.set(mock, field.get(originalServer));
			serverField.set(null, mock);
			origServer = (Server) originalServer;
		} else if (origServer != null)
			serverField.set(null, origServer);
	}

	public Player getPlayer(UUID uuid, @SuperCall Callable<Player> original) {
		try {
			var player = MCChatUtils.LoggedInPlayers.get(uuid);
			if (player == null) return original.call();
			return player;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to handle getPlayer!", e);
			return null;
		}
	}

	public Player getPlayerExact(String name, @SuperCall Callable<Player> original) {
		try {
			var player = MCChatUtils.LoggedInPlayers.values().stream()
				.filter(dcp -> dcp.getName().equalsIgnoreCase(name)).findAny().orElse(null);
			if (player == null) return original.call();
			return player;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to handle getPlayerExact!", e);
			return null;
		}
	}

	@RuntimeType
	//public List<Player> getOnlinePlayers(@SuperCall Callable<List<Player>> original) {
	public List<Player> getOnlinePlayers() {
		try {
			if (playerList == null) {
				//var list = original.call();
				var list = new ArrayList<Player>();
				playerList = new AppendListView<>(list, fakePlayers);
			}
			return playerList;
		} catch (
			Exception e) {
			TBMCCoreAPI.SendException("Failed to handle getOnlinePlayers!", e);
			return null;
		}
	}

	@RuntimeType
	public Object createProfile(UUID uuid, String name) { //Paper's method, casts the player to a CraftPlayer
		try {
			var player = uuid != null ? MCChatUtils.LoggedInPlayers.get(uuid) : null;
			if (player == null && name != null)
				player = MCChatUtils.LoggedInPlayers.values().stream()
					.filter(dcp -> dcp.getName().equalsIgnoreCase(name)).findAny().orElse(null);
			if (player != null)
				return new CraftPlayerProfile(player.getUniqueId(), player.getName());
			return null;
		} catch (Exception e) {
			TBMCCoreAPI.SendException("Failed to handle createProfile!", e);
			return null;
		}
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
