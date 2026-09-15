package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api

import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.dto.WeatherApiResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Service
class WeatherApiService (
    webClientBuilder: WebClient.Builder,

    @Value("\${openweathermap.api.url}") private val apiUrl: String,
    @Value("\${openweathermap.api.key}") private  val apiKey: String
) {
    private val webClient: WebClient = webClientBuilder.baseUrl(apiUrl).build()
    suspend fun getCurrentWeather(lat: Double, lon: Double): WeatherApiResponse {
        return webClient.get()
            .uri {
                uriBuilder -> uriBuilder
                    .queryParam("lat", lat)
                    .queryParam("lon", lon)
                    .queryParam("appid", apiKey)
                    .queryParam("units", "metric")
                    .build()
            }
            .retrieve()
            .awaitBody<WeatherApiResponse>()
    }
}