package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.dto

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonIgnoreProperties(ignoreUnknown = true)
data class IncidentApiResponse(
    val incidents: List<Incident>
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Incident (
        val type: String,
        val properties: Properties,
        val geometry: Geometry
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Geometry (
        val type: String,
        val coordinates: List<Coordinate>
    )

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder("lon", "lat")
    data class Coordinate(
        val lon: Double,
        val lat: Double
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Properties (
        val id: String,
        val iconCategory: String,
        val magnitudeOfDelay: String,
        val from: String,
        val to: String,
        val events: List<Event>
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Event (
        val description: String
    )
}