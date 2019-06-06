package buttondevteam.discordplugin.commands;

import buttondevteam.discordplugin.DiscordPlayer;
import buttondevteam.lib.TBMCCoreAPI;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.TBMCPlayer;
import buttondevteam.lib.player.TBMCPlayerBase;
import com.google.common.collect.HashBiMap;
import lombok.val;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

@CommandClass(helpText = {
	"Connect command", //
	"This command lets you connect your account with a Minecraft account. This allows using the Minecraft chat and other things.", //
})
public class ConnectCommand extends ICommand2DC {

	/**
	 * Key: Minecraft name<br>
	 * Value: Discord ID
	 */
	public static HashBiMap<String, String> WaitingToConnect = HashBiMap.create();

	@Command2.Subcommand
	public boolean def(Command2DCSender sender, String Minecraftname) {
		val message = sender.getMessage();
		val channel = message.getChannel().block();
		val author = message.getAuthor().orElse(null);
		if (author == null || channel == null) return true;
		if (WaitingToConnect.inverse().containsKey(author.getId().asString())) {
			channel.createMessage(
				"Replacing " + WaitingToConnect.inverse().get(author.getId().asString()) + " with " + Minecraftname).subscribe();
			WaitingToConnect.inverse().remove(author.getId().asString());
		}
		@SuppressWarnings("deprecation")
		OfflinePlayer p = Bukkit.getOfflinePlayer(Minecraftname);
		if (p == null) {
			channel.createMessage("The specified Minecraft player cannot be found").subscribe();
			return true;
		}
		try (TBMCPlayer pl = TBMCPlayerBase.getPlayer(p.getUniqueId(), TBMCPlayer.class)) {
			DiscordPlayer dp = pl.getAs(DiscordPlayer.class);
			if (dp != null && author.getId().asString().equals(dp.getDiscordID())) {
				channel.createMessage("You already have this account connected.").subscribe();
				return true;
			}
		} catch (Exception e) {
			TBMCCoreAPI.SendException("An error occured while connecting a Discord account!", e);
			channel.createMessage("An internal error occured!\n" + e).subscribe();
		}
		WaitingToConnect.put(p.getName(), author.getId().asString());
		channel.createMessage(
			"Alright! Now accept the connection in Minecraft from the account " + Minecraftname
				+ " before the next server restart. You can also adjust the Minecraft name you want to connect to with the same command.").subscribe();
		if (p.isOnline())
			((Player) p).sendMessage("§bTo connect with the Discord account " + author.getUsername() + "#"
				+ author.getDiscriminator() + " do /discord accept");
		return true;
	}

}
