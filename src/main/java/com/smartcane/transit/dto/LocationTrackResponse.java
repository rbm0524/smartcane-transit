// com.smartcane.transit.dto.LocationTrackResponse.java
package com.smartcane.transit.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LocationTrackResponse {
    private double distanceMeters;
    private boolean arrived;
    private boolean offRoute;
    private String message;
}
