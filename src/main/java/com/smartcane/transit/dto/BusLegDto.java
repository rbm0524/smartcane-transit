// com/smartcane/transit/dto/BusLegDto.java
package com.smartcane.transit.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class BusLegDto {
    private String busName;      // 노선명(예: 4212)
    private String agency;       // 운수사(있으면)
    private String from;         // 승차 정류장명
    private String to;           // 하차 정류장명
    private Integer durationMin; // 해당 구간 소요(분)
    private Integer stops;       // 정류장 수(있으면)
}
