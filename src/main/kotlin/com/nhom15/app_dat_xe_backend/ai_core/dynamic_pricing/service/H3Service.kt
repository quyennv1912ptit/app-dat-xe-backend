package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.service

import com.uber.h3core.H3Core
import org.springframework.stereotype.Service

@Service
class H3Service (
    private val h3: H3Core
) {
    companion object {
        const val RESOLUTION_MACRO = 8
        const val RESOLUTION_MICRO = 11
    }

    fun getMacroHexId(lat: Double, lon: Double): String {
        return h3.latLngToCellAddress(lat, lon, RESOLUTION_MACRO)
    }

    fun getRouteMicroHexIds(routesCoords: List<Pair<Double, Double>>): Set<String> {
        val routeHexIds = mutableSetOf<String>()
        for (i in 0 until routesCoords.size - 1) {
            val start = routesCoords[i]
            val end = routesCoords[i+1]

            val startHex = h3.latLngToCell(start.first, start.second, RESOLUTION_MICRO)
            val endHex = h3.latLngToCell(end.first, end.second, RESOLUTION_MICRO)

            try {
                val segmentHexes = h3.gridPathCells(startHex, endHex)
                segmentHexes.forEach { hexId ->
                    routeHexIds.add(h3.h3ToString(hexId))
                }
            } catch (e: Exception) {
                println(e.message)
            }
        }
        return routeHexIds
    }

    fun findNeighborHexIds(hexId: String, k: Int = 1): List<String> {
        val originHex = h3.stringToH3(hexId)
        val neighbors = h3.gridDisk(originHex, k)
        return neighbors.map {h3.h3ToString(it)}
    }
}