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
import reactor.core.scala.publisher.javaOptional2ScalaOption

class MCListener extends Listener {
    @EventHandler def onPlayerJoin(e: PlayerJoinEvent): Unit =
        if (ConnectCommand.WaitingToConnect.containsKey(e.getPlayer.getName)) {
            @SuppressWarnings(Array("ConstantConditions")) val user = DiscordPlugin.dc.getUserById(Snowflake.of(ConnectCommand.WaitingToConnect.get(e.getPlayer.getName))).block
            if (user == null) return ()
            e.getPlayer.sendMessage("§bTo connect with the Discord account @" + user.getUsername + "#" + user.getDiscriminator + " do /discord accept")
            e.getPlayer.sendMessage("§bIf it wasn't you, do /discord decline")
        }

    @EventHandler def onGetInfo(e: TBMCPlayerGetInfoEvent): Unit = {
        Option(DiscordPlugin.SafeMode).filterNot(identity).flatMap(_ => Option(e.getPlayer.getAs(classOf[DiscordPlayer])))
            .flatMap(dp => Option(dp.getDiscordID)).filter(_.nonEmpty)
            .map(Snowflake.of).flatMap(id => DiscordPlugin.dc.getUserById(id).onErrorResume(_ => Mono.empty).blockOptional())
            .map(user => {
                e.addInfo("Discord tag: " + user.getUsername + "#" + user.getDiscriminator)
                user
            })
            .flatMap(user => user.asMember(DiscordPlugin.mainServer.getId).onErrorResume(t => Mono.empty).blockOptional())
            .flatMap(member => member.getPresence.blockOptional())
            .map(pr => {
                e.addInfo(pr.getStatus.toString)
                pr
            })
            .flatMap(_.getActivity).foreach(activity => e.addInfo(s"${activity.getType}: ${activity.getName}"))
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