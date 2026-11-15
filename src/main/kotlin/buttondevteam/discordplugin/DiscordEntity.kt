package buttondevteam.discordplugin

import buttondevteam.lib.architecture.config.ConfigDataDelegate
import discord4j.common.util.Snowflake
import discord4j.core.`object`.entity.Entity
import discord4j.core.`object`.entity.Role
import discord4j.core.`object`.entity.channel.MessageChannel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.reactive.asFlow
import kotlin.reflect.KClass

class DiscordEntity<T : Entity>(val data: ConfigDataDelegate<Snowflake>, private val cl: KClass<T>) {
    var id by data
    private var _entity: T? = null

    suspend fun getEntity(): T? {
        val entity = _entity
        if (entity != null && entity.id == id) return entity
        val ret = when (cl) {
            MessageChannel::class -> DiscordPlugin.dc.getChannelById(id).asFlow().firstOrNull() as T?
            Role::class -> DiscordPlugin.dc.getRoleById(DiscordPlugin.plugin!!.mainServer!!.id, id).asFlow().firstOrNull() as T?
            else -> throw Exception("Unexpected type of entity: $cl")
        }
        _entity = ret
        return ret
    }
}