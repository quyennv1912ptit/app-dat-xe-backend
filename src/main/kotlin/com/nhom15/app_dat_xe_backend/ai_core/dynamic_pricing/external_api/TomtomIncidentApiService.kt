package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api

import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.dto.IncidentApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class TomtomIncidentApiService (
    webClientBuilder: WebClient.Builder,

    @Value("\${tomtom.api.url}") private val apiUrl: String,
    @Value("\${tomtom.api.key}") private val apiKey: String,
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(apiUrl).build()

    suspend  fun getCurrentIncidents(minLat: Double, minLon: Double, maxLat: Double, maxLon: Double): IncidentApiResponse {
        val bbox = "$minLon,$minLat,$maxLon,$maxLat"
        return webClient.get()
            .uri{
                uriBuilder -> uriBuilder
                .path("/maps/orbis/traffic/incidents/details")
                .queryParam("apiVersion", "2")
                .queryParam("bbox", bbox)
                .queryParam("timeValidity", "present")
                .build()
            }
            .header("TomTom-Api-Key", apiKey)
            .header("Attributes", "incidents(type,geometry(type,coordinates),properties(id, iconCategory,magnitudeOfDelay, from, to, events(description)))")
            .retrieve()
            .awaitBody<IncidentApiResponse>()
    }
}