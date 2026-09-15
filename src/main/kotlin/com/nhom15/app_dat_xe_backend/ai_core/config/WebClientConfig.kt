package com.nhom15.app_dat_xe_backend.ai_core.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class WebClientConfig {
    @Bean
    @Scope("prototype")
    fun webClientBuilder(): WebClient.Builder {
        return WebClient.builder()
    }
}