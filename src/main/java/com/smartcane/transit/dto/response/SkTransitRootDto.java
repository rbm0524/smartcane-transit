package com.smartcane.transit.dto.response;

import java.util.List;

public record SkTransitRootDto(
        MetaDataDto metaData
) {
    public record MetaDataDto(
            RequestParametersDto requestParameters,
            PlanDto plan
    ) {}
    public record RequestParametersDto(
            int busCount,
            int expressbusCount,
            int subwayCount,
            int airplaneCount,
            int subwayBusCount,
            int trainCount,
            int ferryCount,
            int wideareaRouteCount,
            String locale,
            String startX,
            String startY,
            String endX,
            String endY,
            String reqDttm
    ) {}

    public record PlanDto(
            List<ItineraryDto> itineraries
    ) {}

    public record ItineraryDto(
            FareDto fare,
            int totalTime,
            int totalDistance,
            int totalWalkTime,
            int totalWalkDistance,
            int transferCount,
            int pathType,
            List<LegDto> legs
    ) {}

    public record FareDto(
            RegularFareDto regular
    ) {}

    public record RegularFareDto(
            int totalFare,
            CurrencyDto currency
    ) {}

    public record CurrencyDto(
            String symbol,
            String currency,
            String currencyCode
    ) {}

    public record LegDto(
            String mode,            // WALK / BUS / SUBWAY
            Integer sectionTime,
            Integer distance,
            String routeColor,
            String route,
            String routeId,
            Integer service,
            Integer type,
            PlaceDto start,
            PlaceDto end,
            List<WalkStepDto> steps,       // WALK 전용
            PassStopListDto passStopList,  // BUS, SUBWAY 전용
            PassShapeDto passShape         // BUS, SUBWAY, 일부 WALK
    ) {}

    public record PlaceDto(
            String name,
            Double lon,
            Double lat
    ) {}

    public record WalkStepDto(
            String streetName,
            Integer distance,
            String description,
            String linestring
    ) {}

    public record PassStopListDto(
            List<StationDto> stations
    ) {}

    public record StationDto(
            Integer index,
            String stationName,
            String lon,   // 문자열로 옴
            String lat,
            String stationID
    ) {}

    public record PassShapeDto(
            String linestring
    ) {}
}
