package com.inkr8.data.dto

internal typealias WireMap = Map<String, Any?>
internal fun Any?.wire(): WireMap {
    require(this is Map<*, *>) { "Respuesta de servidor inválida." }
    return entries.associate { (key, value) -> require(key is String); key to value }
}
internal fun WireMap.string(key: String): String = this[key] as? String ?: ""
internal fun WireMap.number(key: String): Long = (this[key] as? Number)?.toLong() ?: 0
internal fun WireMap.maps(key: String): List<WireMap> = (this[key] as? List<*>)?.map { it.wire() }.orEmpty()
internal data class UserDto(val id: String, val name: String, val email: String?, val merit: Long, val rating: Long, val league: Int, val username: String?) {
    companion object {
        fun read(id: String, map: WireMap) = UserDto(id, map.string("name"), map["email"] as? String,
            map.number("merit"), map.number("rating"), map.number("league").toInt(), map["username"] as? String)
    }
}
internal data class GameDto(val id: String, val fields: WireMap)
