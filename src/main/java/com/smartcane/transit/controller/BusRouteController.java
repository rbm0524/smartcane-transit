// com/smartcane/transit/controller/BusRouteController.java
package com.smartcane.transit.controller;

import com.smartcane.transit.dto.BusRouteRequest;
import com.smartcane.transit.dto.BusRouteResponse;
import com.smartcane.transit.service.SkBusRouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transit")
@RequiredArgsConstructor
public class BusRouteController {

    private final SkBusRouteService service;

    /**
     * iOS → 우리 서버로 좌표 전달 → SK API 호출 → "버스 경로만" 추려서 반환
     * POST /api/transit/bus
     */
    @PostMapping("/bus")
    public BusRouteResponse findBus(@Valid @RequestBody BusRouteRequest req) {
        return service.findBusRoutes(req);
    }
}
