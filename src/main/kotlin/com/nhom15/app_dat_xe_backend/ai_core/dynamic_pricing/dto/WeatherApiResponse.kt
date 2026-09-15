package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WeatherApiResponse (
    val weather: List<WeatherDetail>,
    val main: MainDetail
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class WeatherDetail (
        val id: Int,
        val main: String,
        val description: String
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class MainDetail (
        val temp: Double,
        val feels_like: Double,
        val humidity: Int
    )
}