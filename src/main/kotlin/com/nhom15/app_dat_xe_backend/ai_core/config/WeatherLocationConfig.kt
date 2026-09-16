package com.nhom15.app_dat_xe_backend.ai_core.config

import org.springframework.stereotype.Component

@Component
class WeatherLocationConfig {
    val weatherFetchCoordinates: List<Pair<Double, Double>> = listOf(
        Pair(21.028511, 105.804817), // Quận Đống Đa / Ba Đình (Trung tâm)
        Pair(21.028825, 105.852441), // Quận Hoàn Kiếm / Hai Bà Trưng (Khu Phố cổ, ven sông)
        Pair(21.030653, 105.793807), // Quận Cầu Giấy (Khu đông dân cư, văn phòng)
        Pair(20.980816, 105.787498), // Quận Hà Đông / Thanh Xuân (Khu vực dọc Nguyễn Trãi)
        Pair(21.058324, 105.826262), // Quận Tây Hồ (Khu vực phía Bắc)
        Pair(21.012351, 105.765664), // Quận Nam Từ Liêm (Khu vực Mỹ Đình)
        Pair(21.046187, 105.892694), // Quận Long Biên (Bên kia sông Hồng)
        Pair(20.966336, 105.845012)  // Quận Hoàng Mai (Phía Nam)
    )
}