package buttondevteam.discordplugin.mcchat;

import buttondevteam.core.component.channel.Channel;
import buttondevteam.core.component.channel.ChatRoom;
import buttondevteam.discordplugin.*;
import buttondevteam.discordplugin.commands.Command2DCSender;
import buttondevteam.discordplugin.commands.ICommand2DC;
import buttondevteam.lib.TBMCSystemChatEvent;
import buttondevteam.lib.chat.Command2;
import buttondevteam.lib.chat.CommandClass;
import buttondevteam.lib.player.TBMCPlayer;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Permission;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@CommandClass(helpText = {"Channel connect", //
	"This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
	"You need to have access to the MC channel and have manage permissions on the Discord channel.", //
	"You also need to have your Minecraft account connected. In #bot use /connect <mcname>.", //
	"Call this command from the channel you want to use.", //
	"Usage: @Bot channelcon <mcchannel>", //
	"Use the ID (command) of the channel, for example `g` for the global chat.", //
	"To remove a connection use @ChromaBot channelcon remove in the channel.", //
	"Mentioning the bot is needed in this case because the / prefix only works in #bot.", //
	"Invite link: <https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot&permissions=268509264>" //
})
@RequiredArgsConstructor
public class ChannelconCommand extends ICommand2DC {
	private final MinecraftChatModule module;
	@Command2.Subcommand
	public boolean remove(Command2DCSender sender) {
		val message = sender.getMessage();
		if (checkPerms(message)) return true;
		if (MCChatCustom.removeCustomChat(message.getChannelId()))
			DPUtils.reply(message, null, "channel connection removed.").subscribe();
		else
			DPUtils.reply(message, null, "this channel isn't connected.").subscribe();
		return true;
	}

	@Command2.Subcommand
	public boolean toggle(Command2DCSender sender, @Command2.OptionalArg String toggle) {
		val message = sender.getMessage();
		if (checkPerms(message)) return true;
		val cc = MCChatCustom.getCustomChat(message.getChannelId());
		if (cc == null)
			return respond(sender, "this channel isn't connected.");
		Supplier<String> togglesString = () -> Arrays.stream(ChannelconBroadcast.values()).map(t -> t.toString().toLowerCase() + ": " + ((cc.toggles & t.flag) == 0 ? "disabled" : "enabled")).collect(Collectors.joining("\n"))
			+ "\n\n" + TBMCSystemChatEvent.BroadcastTarget.stream().map(target -> target.getName() + ": " + (cc.brtoggles.contains(target) ? "enabled" : "disabled")).collect(Collectors.joining("\n"));
		if (toggle == null) {
			DPUtils.reply(message, null, "toggles:\n" + togglesString.get()).subscribe();
			return true;
		}
		String arg = toggle.toUpperCase();
		val b = Arrays.stream(ChannelconBroadcast.values()).filter(t -> t.toString().equals(arg)).findAny();
		if (!b.isPresent()) {
			val bt = TBMCSystemChatEvent.BroadcastTarget.get(arg);
			if (bt == null) {
				DPUtils.reply(message, null, "cannot find toggle. Toggles:\n" + togglesString.get()).subscribe();
				return true;
			}
			final boolean add;
			if (add = !cc.brtoggles.contains(bt))
				cc.brtoggles.add(bt);
			else
				cc.brtoggles.remove(bt);
			return respond(sender, "'" + bt.getName() + "' " + (add ? "en" : "dis") + "abled");
		}
		//A B | F
		//------- A: original - B: mask - F: new
		//0 0 | 0
		//0 1 | 1
		//1 0 | 1
		//1 1 | 0
		// XOR
		cc.toggles ^= b.get().flag;
		DPUtils.reply(message, null, "'" + b.get().toString().toLowerCase() + "' " + ((cc.toggles & b.get().flag) == 0 ? "disabled" : "enabled")).subscribe();
		return true;
	}

	@Command2.Subcommand
	public boolean def(Command2DCSender sender, String channelID) {
		val message = sender.getMessage();
		if (checkPerms(message)) return true;
		if (MCChatCustom.hasCustomChat(message.getChannelId()))
			return respond(sender, "this channel is already connected to a Minecraft channel. Use `@ChromaBot channelcon remove` to remove it.");
		val chan = Channel.getChannels().filter(ch -> ch.ID.equalsIgnoreCase(channelID) || (Arrays.stream(ch.IDs().get()).anyMatch(cid -> cid.equalsIgnoreCase(channelID)))).findAny();
		if (!chan.isPresent()) { //TODO: Red embed that disappears over time (kinda like the highlight messages in OW)
			DPUtils.reply(message, null, "MC channel with ID '" + channelID + "' not found! The ID is the command for it without the /.").subscribe();
			return true;
		}
		if (!message.getAuthor().isPresent()) return true;
		val author = message.getAuthor().get();
		val dp = DiscordPlayer.getUser(author.getId().asString(), DiscordPlayer.class);
		val chp = dp.getAs(TBMCPlayer.class);
		if (chp == null) {
			DPUtils.reply(message, null, "you need to connect your Minecraft account. On our server in " + DPUtils.botmention() + " do " + DiscordPlugin.getPrefix() + "connect <MCname>").subscribe();
			return true;
		}
		val channel = message.getChannel().block();
		DiscordConnectedPlayer dcp = DiscordConnectedPlayer.create(message.getAuthor().get(), channel, chp.getUUID(), Bukkit.getOfflinePlayer(chp.getUUID()).getName(), module);
		//Using a fake player with no login/logout, should be fine for this event
		String groupid = chan.get().getGroupID(dcp);
		if (groupid == null && !(chan.get() instanceof ChatRoom)) { //ChatRooms don't allow it unless the user joins, which happens later
			DPUtils.reply(message, null, "sorry, you cannot use that Minecraft channel.").subscribe();
			return true;
		}
		if (chan.get() instanceof ChatRoom) { //ChatRooms don't work well
			DPUtils.reply(message, null, "chat rooms are not supported yet.").subscribe();
			return true;
		}
        /*if (MCChatListener.getCustomChats().stream().anyMatch(cc -> cc.groupID.equals(groupid) && cc.mcchannel.ID.equals(chan.get().ID))) {
            DPUtils.reply(message, null, "sorry, this MC chat is already connected to a different channel, multiple channels are not supported atm.");
            return true;
        }*/ //TODO: "Channel admins" that can connect channels?
		MCChatCustom.addCustomChat(channel, groupid, chan.get(), author, dcp, 0, new HashSet<>());
		if (chan.get() instanceof ChatRoom)
			DPUtils.reply(message, null, "alright, connection made to the room!").subscribe();
		else
			DPUtils.reply(message, null, "alright, connection made to group `" + groupid + "`!").subscribe();
		return true;
	}

	@SuppressWarnings("ConstantConditions")
	private boolean checkPerms(Message message) {
		if (!message.getAuthorAsMember().block().getBasePermissions().block().contains(Permission.MANAGE_CHANNELS)) {
			DPUtils.reply(message, null, "you need to have manage permissions for this channel!").subscribe();
			return true;
		}
		return false;
	}

	@Override
	public String[] getHelpText(Method method, Command2.Subcommand ann) {
        return new String[]{ //
	        "Channel connect", //
                "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).", //
                "You need to have access to the MC channel and have manage permissions on the Discord channel.", //
	        "You also need to have your Minecraft account connected. In " + DPUtils.botmention() + " use " + DiscordPlugin.getPrefix() + "connect <mcname>.", //
		        "Call this command from the channel you want to use.", //
			"Usage: " + Objects.requireNonNull(DiscordPlugin.dc.getSelf().block()).getMention() + " channelcon <mcchannel>", //
		        "Use the ID (command) of the channel, for example `g` for the global chat.", //
                "To remove a connection use @ChromaBot channelcon remove in the channel.", //
	        "Mentioning the bot is needed in this case because the " + DiscordPlugin.getPrefix() + " prefix only works in " + DPUtils.botmention() + ".", //
	        "Invite link: <https://discordapp.com/oauth2/authorize?client_id=226443037893591041&scope=bot&permissions=268509264>" // TODO: Set correct client ID
        };
    }
}
