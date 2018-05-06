package buttondevteam.discordplugin;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.PermissibleBase;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IUser;

import java.util.Set;

public class DiscordSender extends DiscordSenderBase implements CommandSender {
	private PermissibleBase perm = new PermissibleBase(this);

	private String name = null;

	public DiscordSender(IUser user, IChannel channel) {
		super(user, channel);
	}

	public DiscordSender(IUser user, IChannel channel, String name) {
		super(user, channel);
		this.name = name;
	}

	@Override
	public boolean isPermissionSet(String name) {
		return perm.isPermissionSet(name);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return this.perm.isPermissionSet(perm);
	}

	@Override
	public boolean hasPermission(String name) {
		if (name.contains("essentials") && !name.equals("essentials.list"))
			return false;
		return perm.hasPermission(name);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return this.perm.hasPermission(perm);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return perm.addAttachment(plugin, name, value);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return perm.addAttachment(plugin);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return perm.addAttachment(plugin, name, value, ticks);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return perm.addAttachment(plugin, ticks);
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
		perm.removeAttachment(attachment);
	}

	@Override
	public void recalculatePermissions() {
		perm.recalculatePermissions();
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return perm.getEffectivePermissions();
	}

	@Override
	public boolean isOp() { // TODO: Connect with TBMC acc
		return false;
	}

	@Override
	public void setOp(boolean value) { // TODO: Connect with TBMC acc
	}

	@Override
	public Server getServer() {
		return Bukkit.getServer();
	}

	@Override
	public String getName() {
		return name == null ? user == null ? "Discord user" : user.getDisplayName(DiscordPlugin.mainServer) : name;
	}

	@Override
	public Spigot spigot() {
		return new CommandSender.Spigot();
	}

}
