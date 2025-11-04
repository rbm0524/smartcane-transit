// com.smartcane.transit.dto.LocationTrackRequest.java
package com.smartcane.transit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LocationTrackRequest {
    @NotNull private Double currentX;
    @NotNull private Double currentY;
    @NotNull private Double destX;
    @NotNull private Double destY;
    private Double thresholdMeters = 3.0;  // 도착 판정 기준 (15m)
}
