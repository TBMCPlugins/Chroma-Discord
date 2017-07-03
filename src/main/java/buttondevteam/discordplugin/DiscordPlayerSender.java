package buttondevteam.discordplugin;

import org.bukkit.entity.*;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import lombok.experimental.Delegate;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class DiscordPlayerSender extends DiscordSenderBase implements Player {
	private @Delegate(excludes = ProjectileSource.class) Player player;

	public DiscordPlayerSender(IUser user, IChannel channel, Player player) {
		super(user, channel);
		this.player = player;
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> arg0) {
		return player.launchProjectile(arg0);
	}

	@Override
	public <T extends Projectile> T launchProjectile(Class<? extends T> arg0, Vector arg1) {
		return player.launchProjectile(arg0, arg1);
	}
}
