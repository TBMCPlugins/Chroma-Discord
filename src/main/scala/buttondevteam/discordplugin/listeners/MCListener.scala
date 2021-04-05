package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.commands.ConnectCommand
import buttondevteam.discordplugin.mcchat.MinecraftChatModule
import buttondevteam.discordplugin.util.DPState
import buttondevteam.discordplugin.{DiscordPlayer, DiscordPlugin}
import buttondevteam.lib.ScheduledServerRestartEvent
import buttondevteam.lib.player.TBMCPlayerGetInfoEvent
import discord4j.common.util.Snowflake
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.{EventHandler, Listener}
import reactor.core.publisher.Mono

class MCListener extends Listener {
    @EventHandler def onPlayerJoin(e: PlayerJoinEvent): Unit =
        if (ConnectCommand.WaitingToConnect.containsKey(e.getPlayer.getName)) {
            @SuppressWarnings(Array("ConstantConditions")) val user = DiscordPlugin.dc.getUserById(Snowflake.of(ConnectCommand.WaitingToConnect.get(e.getPlayer.getName))).block
            if (user == null) return
            e.getPlayer.sendMessage("§bTo connect with the Discord account @" + user.getUsername + "#" + user.getDiscriminator + " do /discord accept")
            e.getPlayer.sendMessage("§bIf it wasn't you, do /discord decline")
        }

    @EventHandler def onGetInfo(e: TBMCPlayerGetInfoEvent): Unit = {
        if (DiscordPlugin.SafeMode) return
        val dp = e.getPlayer.getAs(classOf[DiscordPlayer])
        if (dp == null || dp.getDiscordID == null || dp.getDiscordID == "") return
        val userOpt = DiscordPlugin.dc.getUserById(Snowflake.of(dp.getDiscordID)).onErrorResume(_ => Mono.empty).blockOptional
        if (!userOpt.isPresent) return
        val user = userOpt.get
        e.addInfo("Discord tag: " + user.getUsername + "#" + user.getDiscriminator)
        val memberOpt = user.asMember(DiscordPlugin.mainServer.getId).onErrorResume((t: Throwable) => Mono.empty).blockOptional
        if (!memberOpt.isPresent) return
        val member = memberOpt.get
        val prOpt = member.getPresence.blockOptional
        if (!prOpt.isPresent) return
        val pr = prOpt.get
        e.addInfo(pr.getStatus.toString)
        if (pr.getActivity.isPresent) {
            val activity = pr.getActivity.get
            e.addInfo(activity.getType + ": " + activity.getName)
        }
    }

    /*@EventHandler
      public void onCommandPreprocess(TBMCCommandPreprocessEvent e) {
        if (e.getMessage().equalsIgnoreCase("/stop"))
          MinecraftChatModule.state = DPState.STOPPING_SERVER;
        else if (e.getMessage().equalsIgnoreCase("/restart"))
          MinecraftChatModule.state = DPState.RESTARTING_SERVER;
      }*/ @EventHandler //We don't really need this with the logger stuff but hey
    def onScheduledRestart(e: ScheduledServerRestartEvent): Unit =
        MinecraftChatModule.state = DPState.RESTARTING_SERVER

}