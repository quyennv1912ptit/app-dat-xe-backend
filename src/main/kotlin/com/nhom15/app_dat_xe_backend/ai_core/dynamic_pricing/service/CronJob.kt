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

                        val expirationTime = System.currentTimeMillis() + 1200000L
                        val weatherData = mapOf(
                            "condition" to currentCondition,
                            "surge_factor" to surgeFactor,
                            "description" to currentDescription,
                            "expiresAt" to expirationTime
                        )
                        val weatherJson = objectMapper.writeValueAsString(weatherData)

                        val resultList = mutableListOf<Pair<String, String>>()
                        surroundingHexes.forEach { hexId ->
                            resultList.add(Pair("weather:macro:$hexId", weatherJson))
                        }
                        resultList
                    } catch (e: Exception) {
                        println("Error fetching weather: ${e.message}")
                        emptyList()
                    }
                }
            }

            val allUpdates = mutableMapOf<String, String>()
            deferredResults.awaitAll().forEach { pairList ->
                pairList.forEach { pair ->
                    allUpdates[pair.first] = pair.second
                }
            }

            if (allUpdates.isNotEmpty()) {
                redisTemplate.opsForValue().multiSet(allUpdates)
            }
        }
    }

    @Scheduled(initialDelay = 0, fixedRate = 600000) // 10 phút
    fun fetchAndStoreIncidents() {
        runBlocking(Dispatchers.IO) {
            try {
                val res = incidentApiService.getCurrentIncidents(20.9000, 105.7000, 21.2500, 105.9500)
                val incidents = res.incidents

                val allUpdates = mutableMapOf<String, String>()
                val hexToIncidentsMap = mutableMapOf<String, MutableSet<String>>()

                val expirationTime = System.currentTimeMillis() + 900000L

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
                        "description" to description,
                        "expiresAt" to expirationTime
                    )

                    allUpdates["incident_detail:$incidentId"] = objectMapper.writeValueAsString(incidentDetail)

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

                for ((hexKey, incidentIds) in hexToIncidentsMap) {
                    val hexData = mapOf(
                        "incidentIds" to incidentIds,
                        "expiresAt" to expirationTime
                    )
                    allUpdates[hexKey] = objectMapper.writeValueAsString(hexData)
                }

                if (allUpdates.isNotEmpty()) {
                    redisTemplate.opsForValue().multiSet(allUpdates)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}