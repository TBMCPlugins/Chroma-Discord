package buttondevteam.discordplugin.listeners

import buttondevteam.discordplugin.commands.Command2DCSender
import buttondevteam.discordplugin.{DPUtils, DiscordPlugin}
import buttondevteam.lib.TBMCCoreAPI
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.channel.{MessageChannel, PrivateChannel}
import discord4j.core.`object`.entity.{Member, Message, Role, User}
import reactor.core.publisher.{Flux, Mono}

import java.util.concurrent.atomic.AtomicBoolean

object CommandListener {
    /**
     * Runs a ChromaBot command. If mentionedonly is false, it will only execute the command if it was in #bot with the correct prefix or in private.
     *
     * @param message       The Discord message
     * @param mentionedonly Only run the command if ChromaBot is mentioned at the start of the message
     * @return Whether it <b>did not run</b> the command
     */
    def runCommand(message: Message, commandChannelID: Snowflake, mentionedonly: Boolean): Mono[Boolean] = {
        val timings = CommonListeners.timings
        val ret = Mono.just(true)
        if (message.getContent.isEmpty) return ret //Pin messages and such, let the mcchat listener deal with it
        val content = message.getContent
        timings.printElapsed("A")
        message.getChannel.flatMap((channel: MessageChannel) => {
            def foo(channel: MessageChannel): Mono[Boolean] = {
                var tmp = ret
                if (!mentionedonly) { //mentionedonly conditions are in CommonListeners
                    timings.printElapsed("B")
                    if (!channel.isInstanceOf[PrivateChannel] && !(content.charAt(0) == DiscordPlugin.getPrefix && channel.getId.asLong == commandChannelID.asLong)) { //
                        return ret
                    }
                    timings.printElapsed("C")
                    tmp = ret.`then`(channel.`type`).thenReturn(true) // Fun (this true is ignored - x)
                }
                val cmdwithargs = new StringBuilder(content)
                val gotmention = new AtomicBoolean
                timings.printElapsed("Before self")
                tmp.flatMapMany((x: Boolean) => DiscordPlugin.dc.getSelf.flatMap((self: User) => self.asMember(DiscordPlugin.mainServer.getId)).flatMapMany((self: Member) => {
                    def foo(self: Member): Flux[String] = {
                        timings.printElapsed("D")
                        gotmention.set(checkanddeletemention(cmdwithargs, self.getMention, message))
                        gotmention.set(checkanddeletemention(cmdwithargs, self.getNicknameMention, message) || gotmention.get)
                        val mentions = message.getRoleMentions
                        self.getRoles.filterWhen((r: Role) => mentions.any((rr: Role) => rr.getName == r.getName)).map(_.getMention)
                    }

                    foo(self)
                }).map((mentionRole: String) => {
                    def foo(mentionRole: String): Boolean = {
                        timings.printElapsed("E")
                        gotmention.set(checkanddeletemention(cmdwithargs, mentionRole, message) || gotmention.get) // Delete all mentions
                        !mentionedonly || gotmention.get //Stops here if false
                    }

                    foo(mentionRole)
                }: Boolean)[Mono[Boolean]].switchIfEmpty(Mono.fromSupplier[Boolean](() => !mentionedonly || gotmention.get)))[Mono[Boolean]].filter((b: Boolean) => b).last(false).filter((b: Boolean) => b).doOnNext((b: Boolean) => channel.`type`.subscribe).flatMap((b: Boolean) => {
                    def foo(): Mono[Boolean] = {
                        val cmdwithargsString = cmdwithargs.toString
                        try {
                            timings.printElapsed("F")
                            if (!DiscordPlugin.plugin.manager.handleCommand(new Command2DCSender(message), cmdwithargsString)) return DPUtils.reply(message, channel, "unknown command. Do " + DiscordPlugin.getPrefix + "help for help.").map((_: Message) => false)
                        } catch {
                            case e: Exception =>
                                TBMCCoreAPI.SendException("Failed to process Discord command: " + cmdwithargsString, e, DiscordPlugin.plugin)
                        }
                        Mono.just(false) //If the command succeeded or there was an error, return false
                    }

                    foo()
                }).defaultIfEmpty(true)
            }

            foo(channel)
        })
    }

    private def checkanddeletemention(cmdwithargs: StringBuilder, mention: String, message: Message): Boolean = {
        val prefix = DiscordPlugin.getPrefix
        if (message.getContent.startsWith(mention)) { // TODO: Resolve mentions: Compound arguments, either a mention or text
            if (cmdwithargs.length > mention.length + 1) {
                var i = cmdwithargs.indexOf(" ", mention.length)
                if (i == -1) i = mention.length
                else { //noinspection StatementWithEmptyBody
                    while ( {
                        i < cmdwithargs.length && cmdwithargs.charAt(i) == ' '
                    }) { //Removes any space before the command
                        i += 1
                    }
                }
                cmdwithargs.delete(0, i)
                cmdwithargs.insert(0, prefix) //Always use the prefix for processing
            }
            else cmdwithargs.replace(0, cmdwithargs.length, prefix + "help")
        }
        else {
            if (cmdwithargs.isEmpty) cmdwithargs.replace(0, 0, prefix + "help")
            else if (cmdwithargs.charAt(0) != prefix) cmdwithargs.insert(0, prefix)
            return false //Don't treat / as mention, mentions can be used in public mcchat
        }
        true
    }
}