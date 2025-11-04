// com.smartcane.transit.controller.LocationTrackingController.java
package com.smartcane.transit.controller;

import com.smartcane.transit.dto.LocationTrackRequest;
import com.smartcane.transit.dto.LocationTrackResponse;
import com.smartcane.transit.service.LocationTrackingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationTrackingController {

    private final LocationTrackingService service;

    /**
     * iOS에서 실시간 위치 보고
     * → 서버가 목적지까지 거리 계산 후 메시지 반환
     */
    @PostMapping("/track")
    public LocationTrackResponse track(@Valid @RequestBody LocationTrackRequest req) {
        return service.checkProgress(req);
    }
}
