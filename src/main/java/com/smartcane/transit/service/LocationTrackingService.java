// com.smartcane.transit.service.LocationTrackingService.java
package com.smartcane.transit.service;

import com.smartcane.transit.dto.LocationTrackRequest;
import com.smartcane.transit.dto.LocationTrackResponse;
import org.springframework.stereotype.Service;

@Service
public class LocationTrackingService {

    // Haversine 거리 계산
    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371000; // 지구 반경(m)
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public LocationTrackResponse checkProgress(LocationTrackRequest req) {
        double dist = distanceMeters(req.getCurrentY(), req.getCurrentX(), req.getDestY(), req.getDestX());
        boolean arrived = dist <= req.getThresholdMeters();
        String message;
        if (arrived) message = "목적지에 도착했습니다.";
        else if (dist < 100) message = String.format("목적지까지 약 %.0f미터 남았습니다.", dist);
        else message = String.format("남은 거리 %.0f미터, 직진을 유지하세요.", dist);
        return LocationTrackResponse.builder()
                .distanceMeters(dist)
                .arrived(arrived)
                .offRoute(false) // 향후 경로이탈 검증 추가 가능
                .message(message)
                .build();
    }
}
