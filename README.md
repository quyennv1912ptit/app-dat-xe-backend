# App đặt xe - Pờ tít Go

---

## Kiến trúc sử dụng

*   **Backend Framework:** Spring Boot
*   **Cơ sở dữ liệu**:  PostgreSQL
*   **Bộ nhớ đệm & Thời gian thực:** Redis

---

## Thiết kế databse

### Postgresql

- ``users``
- ``drivers``
- ``trips``
- ``vehicles``
- ``account_relations``
- ``ratings``
- ``wallet_transactions``
- ``device_fingerprints``
---
### Redis
- ``driver:location:driverId``
    - lat
    - lon
    - heading
    - status
- ``supply:macro:hexId``: Các tài xế đang trống trong ô
- ``demand:macro:hexId``: Khách đang tìm xe trong ô
- ``weather:macro:hexId``: thời tiết tại ô đó
    - ``condition``: "Rain"
    - ``surge_factor``: 0.2
- ``surge:macro:hexId``: Hệ số tăng giá tại ô
- ``hex_incidents:hexId``: Mảng Id các sự cố xuất hiện tại ô
- ``incident_detail:Id``: Thông tin chi tiết của sự cố
    - iconCategory
    - from
    - to
    - magnitude
    - description
---

## Các API bên ngoài
*   **Xác thực:** `Firebase Auth`
*   **Thời tiết:** `OpenWeatherMap API`
*   **Bản đồ và Điều hướng:** `Mapbox API`
*   **Sự cố giao thông:** `TomTom Incident API`