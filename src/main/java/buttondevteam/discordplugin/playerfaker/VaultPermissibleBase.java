package buttondevteam.discordplugin.playerfaker;

import java.util.Collections;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

import buttondevteam.discordplugin.DiscordPlugin;

public class VaultPermissibleBase implements Permissible {
	private Player player;
	private OfflinePlayer op;

	public VaultPermissibleBase(Player player) {
		this.player = player;
		op = Bukkit.getOfflinePlayer(player.getUniqueId());
	}

	@Override
	public boolean isPermissionSet(String name) {
		return DiscordPlugin.perms.playerHas(player, name);
	}

	@Override
	public boolean isPermissionSet(Permission perm) {
		return DiscordPlugin.perms.playerHas(player, perm.getName());
	}

	@Override
	public boolean hasPermission(String inName) {
		return DiscordPlugin.perms.playerHas(player, inName);
	}

	@Override
	public boolean hasPermission(Permission perm) {
		return DiscordPlugin.perms.playerHas(player, perm.getName());
	}

	@Override
	public boolean isOp() {
		return op.isOp();
	}

	@Override
	public void setOp(boolean value) {
		op.setOp(value);
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String name, boolean value, int ticks) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int ticks) {
		return null;
	}

	@Override
	public void removeAttachment(PermissionAttachment attachment) {
	}

	@Override
	public void recalculatePermissions() {
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		return Collections.emptySet();
	}
}
