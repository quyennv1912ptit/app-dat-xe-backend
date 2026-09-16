package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.service

import com.nhom15.app_dat_xe_backend.ai_core.config.WeatherLocationConfig
import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.TomtomIncidentApiService
import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.WeatherApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.springframework.data.redis.connection.RedisHashCommands
import org.springframework.data.redis.connection.RedisKeyCommands
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class CronJob(
    private val locationConfig: WeatherLocationConfig,
    private val redisTemplate: StringRedisTemplate,
    private val h3Service: H3Service,
    private val weatherApiService: WeatherApiService,
    private val incidentApiService: TomtomIncidentApiService
) {
    @Scheduled(initialDelay = 0, fixedRate = 900000) // 15 phút
    fun fetchAndStoreWeather() {
        val coordinates = locationConfig.weatherFetchCoordinates

        runBlocking(Dispatchers.IO) {
            val deferredResults = coordinates.map { coord ->
                async {
                    try {
                        val (lat, lon) = coord
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

                        surroundingHexes.map { hexId ->
                            "weather:macro:$hexId" to weatherData
                        }
                    } catch (e: Exception) {
                        println("Error fetching weather: ${e.message}")
                        null
                    }
                }
            }

            val allUpdates = mutableMapOf<String, Map<String, String>>()
            deferredResults.awaitAll().filterNotNull().flatten().forEach { (redisKey, data) ->
                allUpdates[redisKey] = data
            }

            if (allUpdates.isNotEmpty()) {
                val expirationSeconds = 1200L // 20 phút

                redisTemplate.executePipelined(RedisCallback { connection ->
                    val hashCommands = connection.hashCommands()
                    val keyCommands = connection.keyCommands()

                    for ((redisKey, data) in allUpdates) {
                        hashCommands.hMSetAndExpire(keyCommands, redisKey, data, expirationSeconds)
                    }
                    null
                })
            }
        }
    }

    @Scheduled(initialDelay = 0, fixedRate = 100000) // 100 giây
    fun fetchAndStoreIncidents() {
        runBlocking(Dispatchers.IO) {
            try {
                val res = incidentApiService.getCurrentIncidents(20.9000, 105.7000, 21.2500, 105.9500)
                val incidents = res.incidents

                val incidentDetailsMap = mutableMapOf<String, Map<String, String>>()
                val hexToIncidentsMap = mutableMapOf<String, MutableSet<String>>()

                for ((_, properties, geometry) in incidents) {
                    val incidentId = properties.id
                    val description = if (properties.events.isNotEmpty()) properties.events[0].description else ""

                    val incidentDetail = mapOf(
                        "iconCategory" to properties.iconCategory,
                        "magnitude" to properties.magnitudeOfDelay,
                        "from" to properties.from,
                        "to" to properties.to,
                        "description" to description
                    )

                    incidentDetailsMap["incident_detail:$incidentId"] = incidentDetail

                    val coordinates = geometry.coordinates
                    val modifiedCoordinates = coordinates.map { point ->
                        Pair(point.lat, point.lon)
                    }

                    val hexIds = h3Service.getRouteMicroHexIds(modifiedCoordinates)

                    for (hexId in hexIds) {
                        val hexKey = "hex_incidents:$hexId"
                        hexToIncidentsMap.getOrPut(hexKey) { mutableSetOf() }.add(incidentId)
                    }
                }

                if (incidentDetailsMap.isNotEmpty() || hexToIncidentsMap.isNotEmpty()) {
                    val expirationSeconds = 900L // 15 phút

                    redisTemplate.executePipelined(RedisCallback { connection ->
                        val hashCommands = connection.hashCommands()
                        val setCommands = connection.setCommands()
                        val keyCommands = connection.keyCommands()

                        for ((key, details) in incidentDetailsMap) {
                            hashCommands.hMSetAndExpire(keyCommands, key, details, expirationSeconds)
                        }

                        for ((hexKey, incidentIds) in hexToIncidentsMap) {
                            val hexKeyBytes = hexKey.toByteArray(Charsets.UTF_8)
                            val idBytes = incidentIds.map { it.toByteArray(Charsets.UTF_8) }.toTypedArray()

                            keyCommands.del(hexKeyBytes)

                            setCommands.sAdd(hexKeyBytes, *idBytes)
                            keyCommands.expire(hexKeyBytes, expirationSeconds)
                        }
                        null
                    })
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

private fun RedisHashCommands.hMSetAndExpire(
    keyCommands: RedisKeyCommands,
    key: String,
    details: Map<String, String>,
    expirationSeconds: Long
) {
    val keyBytes = key.toByteArray(Charsets.UTF_8)
    val hashBytes = details
        .mapKeys { it.key.toByteArray(Charsets.UTF_8) }
        .mapValues { it.value.toByteArray(Charsets.UTF_8) }

    this.hMSet(keyBytes, hashBytes)
    keyCommands.expire(keyBytes, expirationSeconds)
}