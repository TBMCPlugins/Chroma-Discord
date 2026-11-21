package buttondevteam.discordplugin

import buttondevteam.lib.architecture.config.ConfigDataDelegate
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Entity
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.channel.MessageChannel
import discord4j.rest.http.client.ClientException
import kotlinx.coroutines.reactive.awaitFirstOrNull

interface DiscordEntity<T : Entity> {
    var id: Snowflake
    val data: ConfigDataDelegate<Snowflake>
    suspend fun getEntity(): T?;
}

class DiscordGuildData(override val data: ConfigDataDelegate<Snowflake>) : DiscordEntity<Guild> {
    override var id by data
    private var _entity: Guild? = null

    override suspend fun getEntity(): Guild? {
        if (id.asLong() == 0L) return null
        val entity = _entity
        if (entity != null && entity.id == id) return entity
        try {
            val ret = DiscordPlugin.dc.getGuildById(id).awaitFirstOrNull()
            _entity = ret
            return ret
        } catch (ex: ClientException) {
            ex.printStackTrace()
            return null
        }
    }
}

class DiscordChannelData(override val data: ConfigDataDelegate<Snowflake>) : DiscordEntity<MessageChannel> {
    override var id by data
    private var _entity: MessageChannel? = null

    override suspend fun getEntity(): MessageChannel? {
        if (id.asLong() == 0L) return null
        val entity = _entity
        if (entity != null && entity.id == id) return entity
        try {
            val ret = DiscordPlugin.dc.getChannelById(id).awaitFirstOrNull()
            if (ret != null && ret !is MessageChannel)
                throw ClassCastException("The config value for ${data.path} needs to be a message channel, got: ${ret::class.simpleName}")
            _entity = ret
            return ret
        } catch (ex: ClientException) {
            ex.printStackTrace()
            return null
        }
    }
}

class DiscordRoleData(override val data: ConfigDataDelegate<Snowflake>, val guildGetter: suspend () -> Guild?) :
    DiscordEntity<Role> {
    override var id by data
    private var _entity: Role? = null
    private var _guild: Guild? = null

    override suspend fun getEntity(): Role? {
        if (id.asLong() == 0L) return null
        val entity = _entity
        if (entity != null && entity.id == id) return entity
        // If the guild is not set correctly we can't get the role, it's fine for some use-cases
        val guild = _guild ?: guildGetter() ?: return null
        try {
            val ret = DiscordPlugin.dc.getRoleById(guild.id, id).awaitFirstOrNull()
            _entity = ret
            return ret
        } catch (ex: ClientException) {
            ex.printStackTrace()
            return null
        }
    }
}
