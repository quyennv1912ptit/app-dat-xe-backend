package com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.controller

import com.nhom15.app_dat_xe_backend.ai_core.dynamic_pricing.service.PricingService
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/pricing")
class PricingController(
    private val pricingService: PricingService
) {
    @RequestMapping("/estimate")
    fun getPriceEstimate(): String {
        return pricingService.calculatePrice()
    }
}