// com/smartcane/transit/dto/BusRouteResponse.java
package com.smartcane.transit.dto;

import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusRouteResponse {
    private List<BusRouteDto> routes; // 버스 경로만 리스트
    private String source;            // "tmap-path" 또는 "plan-itineraries" 등
    private String rawSample;         // 디버깅용(첫 경로 원본 일부, 운영시 제거 가능)
}
