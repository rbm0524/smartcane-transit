package com.smartcane.transit.service;

import com.smartcane.transit.dto.response.SkTransitRootDto.ItineraryDto;

import java.util.Comparator;
import java.util.List;

public class SkRouteSelector {

    /**
     * SK API에서 받은 전체 itineraries 중에서
     * 1순위: 버스 위주(pathType == 2)
     * 2순위: 지하철+버스(pathType == 3)
     * 3순위: 그 외 전체
     *
     * + 각 그룹 안에서는 totalTime 오름차순 정렬
     * + 필요하면 .limit(3) 같은 걸로 개수 제한 가능
     */
    public List<ItineraryDto> selectPreferredItineraries(List<ItineraryDto> all) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }

        // 1) 버스 위주 (pathType=2)
        List<ItineraryDto> busOnly = all.stream()
                .filter(it -> it.pathType() == 2)
                .sorted(Comparator.comparingInt(ItineraryDto::totalTime))
                .toList();

        if (!busOnly.isEmpty()) {
            // 여기서 상위 3개만 보내고 싶으면 .stream().limit(3).toList()로 감싸
            return busOnly;
        }

        // 2) 지하철+버스 (pathType=3)
        List<ItineraryDto> subwayBus = all.stream()
                .filter(it -> it.pathType() == 3)
                .sorted(Comparator.comparingInt(ItineraryDto::totalTime))
                .toList();

        if (!subwayBus.isEmpty()) {
            return subwayBus;
        }

        // 3) 둘 다 없으면 그냥 전체를 시간 순으로
        return all.stream()
                .sorted(Comparator.comparingInt(ItineraryDto::totalTime))
                .toList();
    }
}
