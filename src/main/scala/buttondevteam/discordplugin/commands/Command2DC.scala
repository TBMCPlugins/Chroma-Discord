package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.Command2
import discord4j.common.util.Snowflake
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.{ApplicationCommandOptionData, ApplicationCommandRequest}

import java.lang.reflect.Method

class Command2DC extends Command2[ICommand2DC, Command2DCSender]('/', false) {
    override def registerCommand(command: ICommand2DC): Unit = {
        registerCommand(command, DiscordPlugin.dc.getApplicationInfo.block().getId.asLong())
    }

    def registerCommand(command: ICommand2DC, appId: Long, guildId: Option[Long] = None): Unit = {
        super.registerCommand(command) //Needs to be configurable for the helps
        val greetCmdRequest = ApplicationCommandRequest.builder()
            .name(command.getCommandPath) //TODO: Main path
            .description("Connect your Minecraft account.") //TODO: Description
            .addOption(ApplicationCommandOptionData.builder()
                .name("name")
                .description("Your name")
                .`type`(ApplicationCommandOption.Type.STRING.getValue)
                .required(true)
                .build()
            ).build()
        val service = DiscordPlugin.dc.getRestClient.getApplicationService
        guildId match {
            case Some(id) => service.createGuildApplicationCommand(appId, id, greetCmdRequest).subscribe()
            case None => service.createGlobalApplicationCommand(appId, greetCmdRequest).subscribe()
        }
    }

    override def hasPermission(sender: Command2DCSender, command: ICommand2DC, method: Method): Boolean = {
        //return !command.isModOnly() || sender.getMessage().getAuthor().hasRole(DiscordPlugin.plugin.modRole().get()); //TODO: modRole may be null; more customisable way?
        true
    }
}