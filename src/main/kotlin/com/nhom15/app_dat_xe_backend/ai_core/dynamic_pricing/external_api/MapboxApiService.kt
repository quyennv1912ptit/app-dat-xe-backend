package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api

import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.dto.MapboxApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class MapboxApiService (
    webClientBuilder: WebClient.Builder,

    @Value("\${mapbox.api.navigation.url}") private val apiUrl: String,
    @Value("\${mapbox.api.navigation.key}") private  val apiKey: String
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(apiUrl).build()

    suspend fun getCurrentDistanceAndTime(startLat: Double, startLon: Double, endLat: Double, endLon: Double): MapboxApiResponse {
        val coordinates: String = "$startLon,$startLat;$endLon,$endLat"
        return webClient.get()
            .uri {
                uriBuilder -> uriBuilder
                .path("/directions/v5/mapbox/driving-traffic/{coordinates}")
                .queryParam("access_token", apiKey)
                .queryParam("exclude", "motorway")
                .queryParam("overview", "false")
                .build(coordinates)}
            .retrieve()
            .awaitBody<MapboxApiResponse>()
    }
}