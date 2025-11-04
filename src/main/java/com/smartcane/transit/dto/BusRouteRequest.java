// com/smartcane/transit/dto/BusRouteRequest.java
package com.smartcane.transit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusRouteRequest {
    @NotNull private Double startX;
    @NotNull private Double startY;
    @NotNull private Double endX;
    @NotNull private Double endY;

    // 옵션(기본값 제공)
    private Integer lang;        // default 0
    private String  format;      // default "json"
    private Integer count;       // default 10
    private String  searchDttm;  // default: now(yyyyMMddHHmm)
}
