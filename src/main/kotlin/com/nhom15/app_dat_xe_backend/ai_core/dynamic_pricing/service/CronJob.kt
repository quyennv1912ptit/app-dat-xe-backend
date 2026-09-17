package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.service

import tools.jackson.databind.json.JsonMapper
import com.nhom15.app_dat_xe_backend.ai_core.config.WeatherLocationConfig
import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.TomtomIncidentApiService
import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.data.redis.core.script.RedisScript
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CronJob(
    private val locationConfig: WeatherLocationConfig,
    private val redisTemplate: StringRedisTemplate,
    private val h3Service: H3Service,
    private val weatherApiService: WeatherApiService,
    private val incidentApiService: TomtomIncidentApiService,
    private val objectMapper: JsonMapper
) {

    companion object {
        private const val HASH_SET_WITH_TTL_LUA = """
            local hashKey = KEYS[1]
            local ttl = tonumber(ARGV[1])
            redis.call('HSET', hashKey, unpack(ARGV, 2, #ARGV))
            redis.call('EXPIRE', hashKey, ttl)
            return 1
        """

        private val hashSetWithTtlScript: RedisScript<Long> =
            DefaultRedisScript(HASH_SET_WITH_TTL_LUA.trimIndent(), Long::class.java)

        const val WEATHER_HASH_KEY = "weather:macro"
        const val INCIDENT_DETAIL_HASH_KEY = "incident_detail"
        const val HEX_INCIDENTS_HASH_KEY = "hex_incidents"
    }

    private fun hashSetWithTtl(hashKey: String, fields: Map<String, String>, ttlSeconds: Long) {
        if (fields.isEmpty()) return

        val entries = fields.entries.toList()
        for (chunk in entries.chunked(2000)) {
            val args = ArrayList<String>(1 + chunk.size * 2)
            args.add(ttlSeconds.toString())
            chunk.forEach { entry ->
                args.add(entry.key)
                args.add(entry.value)
            }

            redisTemplate.execute(hashSetWithTtlScript, listOf(hashKey), *args.toTypedArray())
        }
    }

    @Scheduled(initialDelay = 0, fixedRate = 900000) // 15 phút
    fun fetchAndStoreWeather() {
        val coordinates = locationConfig.weatherFetchCoordinates

        runBlocking(Dispatchers.IO) {
            val deferredResults = coordinates.map { coord ->
                async<List<Pair<String, String>>> {
                    try {
                        val lat = coord.first
                        val lon = coord.second
                        val res = weatherApiService.getCurrentWeather(lat, lon)

                        val currentCondition = res.weather.firstOrNull()?.main ?: "Unknown"
                        val currentDescription = res.weather.firstOrNull()?.description ?: "Unknown"
                        val surgeFactor = "0.2"

                        val centerHexId = h3Service.getMacroHexId(lat, lon)
                        val surroundingHexes = h3Service.findNeighborHexIds(centerHexId, 5)

                        val weatherData = mapOf(
                            "condition" to currentCondition,
                            "surge_factor" to surgeFactor,
                            "description" to currentDescription
                        )
                        val weatherJson = objectMapper.writeValueAsString(weatherData)

                        surroundingHexes.map { hexId -> Pair(hexId, weatherJson) }
                    } catch (e: Exception) {
                        println("Error fetching weather: ${e.message}")
                        emptyList()
                    }
                }
            }

            val allFields = mutableMapOf<String, String>()
            deferredResults.awaitAll().forEach { pairList ->
                pairList.forEach { (hexId, json) -> allFields[hexId] = json }
            }

            hashSetWithTtl(WEATHER_HASH_KEY, allFields, ttlSeconds = 1200)
        }
    }

    @Scheduled(initialDelay = 0, fixedRate = 600000) // 10 phút
    fun fetchAndStoreIncidents() {
        runBlocking(Dispatchers.IO) {
            try {
                val res = incidentApiService.getCurrentIncidents(20.9000, 105.7000, 21.2500, 105.9500)
                val incidents = res.incidents

                val incidentDetailFields = mutableMapOf<String, String>()
                val hexToIncidentsMap = mutableMapOf<String, MutableSet<String>>()

                for (incident in incidents) {
                    val properties = incident.properties
                    val geometry = incident.geometry

                    val incidentId = properties.id
                    val description = if (properties.events.isNotEmpty()) properties.events[0].description else ""

                    val incidentDetail = mapOf(
                        "iconCategory" to properties.iconCategory,
                        "magnitude" to properties.magnitudeOfDelay,
                        "from" to properties.from,
                        "to" to properties.to,
                        "description" to description
                    )

                    incidentDetailFields[incidentId] = objectMapper.writeValueAsString(incidentDetail)

                    val coordinates = geometry.coordinates
                    val modifiedCoordinates = coordinates.map { point ->
                        Pair(point.lat, point.lon)
                    }

                    val hexIds = h3Service.getRouteMicroHexIds(modifiedCoordinates)

                    for (hexId in hexIds) {
                        hexToIncidentsMap.getOrPut(hexId) { mutableSetOf() }.add(incidentId)
                    }
                }

                val hexIncidentFields = mutableMapOf<String, String>()
                for ((hexId, incidentIds) in hexToIncidentsMap) {
                    val hexData = mapOf("incidentIds" to incidentIds)
                    hexIncidentFields[hexId] = objectMapper.writeValueAsString(hexData)
                }

                hashSetWithTtl(INCIDENT_DETAIL_HASH_KEY, incidentDetailFields, ttlSeconds = 900)
                hashSetWithTtl(HEX_INCIDENTS_HASH_KEY, hexIncidentFields, ttlSeconds = 900)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}