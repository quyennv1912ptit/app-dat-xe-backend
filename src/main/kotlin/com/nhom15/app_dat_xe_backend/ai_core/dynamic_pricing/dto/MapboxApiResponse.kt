package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MapboxApiResponse (
    val routes: List<Routes>
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Routes (
        val distance: Double,
        val duration: Double,
        val duration_typical: Double
    )
}