package com.nhom15.app_dat_xe_backend.ai_core.config

import com.uber.h3core.H3Core
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class H3Config {
    @Bean
    fun h3Core(): H3Core {
        return H3Core.newInstance()
    }
}