package com.nhom15.app_dat_xe_backend

import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.MapboxApiService
import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.TomtomIncidentApiService
import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.external_api.WeatherApiService
import kotlinx.coroutines.runBlocking
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean

@SpringBootApplication
class AppDatXeBackendApplication {
	@Bean
	fun testAllApis(
		weatherApiService: WeatherApiService,
		tomtomIncidentApiService: TomtomIncidentApiService,
		mapboxApiService: MapboxApiService
	) = CommandLineRunner {
		runBlocking {

			println("====== BẮT ĐẦU TEST CÁC API ======")

			try {
				println("--- 1. API THỜI TIẾT ---")
				val weather = weatherApiService.getCurrentWeather(20.957478, 105.803112)
				println(weather)

				println("\n--- 2. API SỰ CỐ ---")
				val incidents = tomtomIncidentApiService.getCurrentIncidents(20.9808, 105.7874, 21.0480, 105.8369)
				println(incidents)

				println("\n--- 3. API MAPBOX ---")
				val mapbox = mapboxApiService.getCurrentDistanceAndTime(20.9808, 105.7874, 21.0480, 105.8369)
				println(mapbox)

			} catch (e: Exception) {
				println("Lỗi khi gọi API: ${e.message}")
			}

			println("==================================")
		}
	}
}

fun main(args: Array<String>) {
	runApplication<AppDatXeBackendApplication>(*args)
}
