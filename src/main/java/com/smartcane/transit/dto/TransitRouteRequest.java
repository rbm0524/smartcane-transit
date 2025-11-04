// src/main/java/com/smartcane/transit/dto/TransitRouteRequest.java
package com.smartcane.transit.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransitRouteRequest {

    @NotNull
    private Double startX;
    @NotNull
    private Double startY;
    @NotNull
    private Double endX;
    @NotNull
    private Double endY;

    // SK API 파라미터들 (옵션)
    // lang: 0(ko) 기본, 1(en) 등
    private Integer lang;        // default 0
    // format: "json" 권장
    private String format;       // default "json"
    // count: 결과 개수
    private Integer count;       // default 10
    // 검색 시각(예: 202301011200) — 미지정 시 현재 시간(yyyyMMddHHmm)로 대체
    private String searchDttm;   // optional
}
