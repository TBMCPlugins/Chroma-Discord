package buttondevteam.discordplugin.mcchat

import buttondevteam.core.component.channel.{Channel, ChatRoom}
import buttondevteam.discordplugin.*
import buttondevteam.discordplugin.ChannelconBroadcast.ChannelconBroadcast
import buttondevteam.discordplugin.commands.{Command2DCSender, ICommand2DC}
import buttondevteam.discordplugin.mcchat.sender.{DiscordConnectedPlayer, DiscordPlayer}
import buttondevteam.lib.TBMCSystemChatEvent
import buttondevteam.lib.chat.{Command2, CommandClass}
import buttondevteam.lib.player.{ChromaGamerBase, TBMCPlayer}
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.channel.{GuildChannel, MessageChannel}
import discord4j.rest.util.{Permission, PermissionSet}
import org.bukkit.Bukkit
import reactor.core.scala.publisher.SMono

import java.lang.reflect.Method
import java.util
import java.util.function.Supplier
import java.util.stream.Collectors
import java.util.{Objects, Optional}
import javax.annotation.Nullable

@SuppressWarnings(Array("SimplifyOptionalCallChains")) //Java 11
@CommandClass(helpText = Array("Channel connect", //
    "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).",
    "You need to have access to the MC channel and have manage permissions on the Discord channel.",
    "You also need to have your Minecraft account connected. In #bot use /connect <mcname>.",
    "Call this command from the channel you want to use.", "Usage: @Bot channelcon <mcchannel>",
    "Use the ID (command) of the channel, for example `g` for the global chat.",
    "To remove a connection use @ChromaBot channelcon remove in the channel.",
    "Mentioning the bot is needed in this case because the / prefix only works in #bot.",
    "Invite link: <Unknown>" //
))
class ChannelconCommand(private val module: MinecraftChatModule) extends ICommand2DC {
    @Command2.Subcommand def remove(sender: Command2DCSender): Boolean = {
        val message: Message = null // TODO
        if (checkPerms(message, null)) return true
        else if (MCChatCustom.removeCustomChat(message.getChannelId))
            DPUtils.reply(message, SMono.empty, "channel connection removed.").subscribe()
        else
            DPUtils.reply(message, SMono.empty, "this channel isn't connected.").subscribe()
        true
    }

    @Command2.Subcommand def toggle(sender: Command2DCSender, @Command2.OptionalArg toggle: String): Boolean = {
        val message: Message = null // TODO
        if (checkPerms(message, null)) {
            return true
        }
        val cc: MCChatCustom.CustomLMD = MCChatCustom.getCustomChat(message.getChannelId)
        if (cc == null) {
            return respond(sender, "this channel isn't connected.")
        }
        val togglesString: Supplier[String] = () => ChannelconBroadcast.values
            .map(t => t.toString.toLowerCase + ": " + (if ((cc.toggles & (1 << t.id)) == 0) "disabled" else "enabled"))
            .mkString("\n") + "\n\n" +
            TBMCSystemChatEvent.BroadcastTarget.stream.map((target: TBMCSystemChatEvent.BroadcastTarget) =>
                target.getName + ": " + (if (cc.brtoggles.contains(target)) "enabled" else "disabled"))
                .collect(Collectors.joining("\n"))
        if (toggle == null) {
            DPUtils.reply(message, SMono.empty, "toggles:\n" + togglesString.get).subscribe()
            return true
        }
        val arg: String = toggle.toUpperCase
        val b = ChannelconBroadcast.values.find((t: ChannelconBroadcast) => t.toString == arg)
        if (b.isEmpty) {
            val bt: TBMCSystemChatEvent.BroadcastTarget = TBMCSystemChatEvent.BroadcastTarget.get(arg)
            if (bt == null) {
                DPUtils.reply(message, SMono.empty, "cannot find toggle. Toggles:\n" + togglesString.get).subscribe()
                return true
            }
            val add: Boolean = !(cc.brtoggles.contains(bt))
            if (add) {
                cc.brtoggles += bt
            }
            else {
                cc.brtoggles -= bt
            }
            return respond(sender, "'" + bt.getName + "' " + (if (add) "en" else "dis") + "abled")
        }
        //A B | F
        //------- A: original - B: mask - F: new
        //0 0 | 0
        //0 1 | 1
        //1 0 | 1
        //1 1 | 0
        // XOR
        cc.toggles ^= (1 << b.get.id)
        DPUtils.reply(message, SMono.empty, "'" + b.get.toString.toLowerCase + "' "
            + (if ((cc.toggles & (1 << b.get.id)) == 0) "disabled" else "enabled")).subscribe()
        true
    }

    @Command2.Subcommand def `def`(sender: Command2DCSender, channelID: String): Boolean = {
        val message: Message = null // TODO
        if (!(module.allowCustomChat.get)) {
            sender.sendMessage("channel connection is not allowed on this Minecraft server.")
            return true
        }
        val channel = message.getChannel.block
        if (checkPerms(message, channel)) {
            return true
        }
        if (MCChatCustom.hasCustomChat(message.getChannelId)) {
            return respond(sender, "this channel is already connected to a Minecraft channel. Use `@ChromaBot channelcon remove` to remove it.")
        }
        val chan: Optional[Channel] = Channel.getChannels.filter((ch: Channel) => ch.ID.equalsIgnoreCase(channelID) || (util.Arrays.stream(ch.IDs.get).anyMatch((cid: String) => cid.equalsIgnoreCase(channelID)))).findAny
        if (!(chan.isPresent)) { //TODO: Red embed that disappears over time (kinda like the highlight messages in OW)
            DPUtils.reply(message, channel, "MC channel with ID '" + channelID + "' not found! The ID is the command for it without the /.").subscribe()
            return true
        }
        if (!(message.getAuthor.isPresent)) {
            return true
        }
        val author = message.getAuthor.get
        val dp: DiscordPlayer = ChromaGamerBase.getUser(author.getId.asString, classOf[DiscordPlayer])
        val chp: TBMCPlayer = dp.getAs(classOf[TBMCPlayer])
        if (chp == null) {
            DPUtils.reply(message, channel, "you need to connect your Minecraft account. On the main server in " + DPUtils.botmention + " do " + DiscordPlugin.getPrefix + "connect <MCname>").subscribe()
            return true
        }
        val dcp: DiscordConnectedPlayer = DiscordConnectedPlayer.create(message.getAuthor.get, channel, chp.getUUID, Bukkit.getOfflinePlayer(chp.getUUID).getName, module)
        //Using a fake player with no login/logout, should be fine for this event
        val groupid: String = chan.get.getGroupID(dcp)
        if (groupid == null && !((chan.get.isInstanceOf[ChatRoom]))) { //ChatRooms don't allow it unless the user joins, which happens later
            DPUtils.reply(message, channel, "sorry, you cannot use that Minecraft channel.").subscribe()
            return true
        }
        if (chan.get.isInstanceOf[ChatRoom]) { //ChatRooms don't work well
            DPUtils.reply(message, channel, "chat rooms are not supported yet.").subscribe()
            return true
        }
        /*if (MCChatListener.getCustomChats().stream().anyMatch(cc -> cc.groupID.equals(groupid) && cc.mcchannel.ID.equals(chan.get().ID))) {
                    DPUtils.reply(message, null, "sorry, this MC chat is already connected to a different channel, multiple channels are not supported atm.");
                    return true;
                }*/
        //TODO: "Channel admins" that can connect channels?
        MCChatCustom.addCustomChat(channel, groupid, chan.get, author, dcp, 0, Set())
        if (chan.get.isInstanceOf[ChatRoom]) {
            DPUtils.reply(message, channel, "alright, connection made to the room!").subscribe()
        }
        else {
            DPUtils.reply(message, channel, "alright, connection made to group `" + groupid + "`!").subscribe()
        }
        true
    }

    @SuppressWarnings(Array("ConstantConditions"))
    private def checkPerms(message: Message, @Nullable channel: MessageChannel): Boolean = {
        if (channel == null) {
            return checkPerms(message, message.getChannel.block)
        }
        if (!((channel.isInstanceOf[GuildChannel]))) {
            DPUtils.reply(message, channel, "you can only use this command in a server!").subscribe()
            return true
        }
        //noinspection OptionalGetWithoutIsPresent
        val perms: PermissionSet = (channel.asInstanceOf[GuildChannel]).getEffectivePermissions(message.getAuthor.map(_.getId).get).block
        if (!(perms.contains(Permission.ADMINISTRATOR)) && !(perms.contains(Permission.MANAGE_CHANNELS))) {
            DPUtils.reply(message, channel, "you need to have manage permissions for this channel!").subscribe()
            return true
        }
        false
    }

    override def getHelpText(method: Method, ann: Command2.Subcommand): Array[String] =
        Array[String](
            "Channel connect",
            "This command allows you to connect a Minecraft channel to a Discord channel (just like how the global chat is connected to #minecraft-chat).",
            "You need to have access to the MC channel and have manage permissions on the Discord channel.",
            "You also need to have your Minecraft account connected. In " + DPUtils.botmention + " use " + DiscordPlugin.getPrefix + "connect <mcname>.",
            "Call this command from the channel you want to use.", "Usage: " + Objects.requireNonNull(DiscordPlugin.dc.getSelf.block).getMention + " channelcon <mcchannel>",
            "Use the ID (command) of the channel, for example `g` for the global chat.",
            "To remove a connection use @ChromaBot channelcon remove in the channel.",
            "Mentioning the bot is needed in this case because the " + DiscordPlugin.getPrefix + " prefix only works in " + DPUtils.botmention + ".",
            "Invite link: <https://discordapp.com/oauth2/authorize?client_id="
                + SMono(DiscordPlugin.dc.getApplicationInfo).map(info => info.getId.asString).blockOption().getOrElse("Unknown")
                + "&scope=bot&permissions=268509264>")
}