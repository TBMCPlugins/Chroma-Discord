package buttondevteam.discordplugin.commands

import buttondevteam.discordplugin.DiscordPlugin
import buttondevteam.lib.chat.Command2
import buttondevteam.lib.chat.commands.SubcommandData
import discord4j.core.`object`.command.ApplicationCommandOption
import discord4j.discordjson.json.ApplicationCommandOptionData
import discord4j.discordjson.json.ApplicationCommandRequest

class Command2DC : Command2<ICommand2DC, Command2DCSender>('/', false) {
    override fun registerCommand(command: ICommand2DC) {
        registerCommand(command, DiscordPlugin.dc.applicationInfo.block()!!.id.asLong()) // TODO: block() -> suspend
    }

    fun registerCommand(command: ICommand2DC, appId: Long, guildId: Long? = null) {
        val mainNode = super.registerCommandSuper(command) //Needs to be configurable for the helps
        println("Main node name is " + mainNode[0].getName())
        // TODO: Go through all subcommands and register them
        val greetCmdRequest = ApplicationCommandRequest.builder()
            .name(mainNode.getName)
            .description("Connect your Minecraft account.") //TODO: Description
            .addOption(ApplicationCommandOptionData.builder()
                .name("name")
                .description("Your name")
                .type(ApplicationCommandOption.Type.STRING.value)
                .required(true)
                .build()
            ).build()
        val service = DiscordPlugin.dc.restClient.applicationService
        when (guildId) {
            is Long -> service.createGuildApplicationCommand(appId, guildId, greetCmdRequest).subscribe()
            null -> service.createGlobalApplicationCommand(appId, greetCmdRequest).subscribe()
        }
    }

    override fun hasPermission(sender: Command2DCSender, data: SubcommandData<ICommand2DC, Command2DCSender>): Boolean {
        //return !command.isModOnly() || sender.getMessage().getAuthor().hasRole(DiscordPlugin.plugin.modRole().get()); //TODO: modRole may be null; more customisable way?
        return true
    }
}