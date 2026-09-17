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
- ``driver:state`` HASH: driverId - JSON: {lat, lon, status, updatedAt}

- ``driver:current_hex`` HASH : driverId - hexId

- ``supply:hexId`` ZSET : driverId - timestamp

- ``demand:hexId`` ZSET : customerId - timestamp

- ``weather:macro`` HASH : hexId - JSON: {"condition": "Rain", "surge_factor": "0.2", "description": "moderate rain"}

- ``surge:macro`` HASH : hexId - Float: 1.5

- ``hex_incidents`` HASH : hexId - JSON: {"incidentIds": ["TTI-6804d47b-d6c6-435d-847f-e8c860a43716-TTR85749350744344000"]}

- ``incident_detail`` HASH : incidentId - JSON: {"iconCategory": "jam", "magnitude": "minor", "from": "Nội Bài", "to": "Phố Kim Anh / Đường Kim Anh", "description": "Slow traffic"}
---

## Các API bên ngoài
*   **Xác thực:** `Firebase Auth`
*   **Thời tiết:** `OpenWeatherMap API`
*   **Bản đồ và Điều hướng:** `Mapbox API`
*   **Sự cố giao thông:** `TomTom Incident API`