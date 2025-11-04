// com/smartcane/transit/dto/BusRouteDto.java
package com.smartcane.transit.dto;

import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusRouteDto {
    private Integer totalTimeMin;     // 총 소요(분)
    private Integer fare;             // 총 요금(원) (있으면)
    private Integer transferCount;    // 환승 횟수
    private List<BusLegDto> legs;     // 버스 탑승 구간들만
}
