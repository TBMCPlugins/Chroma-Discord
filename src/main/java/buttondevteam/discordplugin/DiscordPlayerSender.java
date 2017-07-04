package buttondevteam.discordplugin;

import java.util.Set;

import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import lombok.experimental.Delegate;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

public class DiscordPlayerSender extends DiscordSenderBase implements Player {

	protected @Delegate(excludes = ProjectileSource.class) Player player;
	// protected @Delegate(excludes = { ProjectileSource.class, Permissible.class }) Player player;
	// protected @Delegate(excludes = { ProjectileSource.class, CommandSender.class }) Player player;

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
