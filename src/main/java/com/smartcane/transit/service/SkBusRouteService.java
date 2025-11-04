// com/smartcane/transit/service/SkBusRouteService.java
package com.smartcane.transit.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcane.transit.config.SkTransitProperties;
import com.smartcane.transit.dto.*;
import lombok.RequiredArgsConstructor;
import okhttp3.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkBusRouteService {

    private final SkTransitProperties props;
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper om = new ObjectMapper();

    public BusRouteResponse findBusRoutes(BusRouteRequest req) {
        String url = props.getBaseUrl();
        if (url == null || url.isBlank()) throw new IllegalStateException("SK base-url 미설정");
        if (props.getAppKey() == null || props.getAppKey().isBlank()) throw new IllegalStateException("SK appKey 미설정");

        // 기본값
        int lang = (req.getLang() == null) ? 0 : req.getLang();
        String format = (req.getFormat() == null || req.getFormat().isBlank()) ? "json" : req.getFormat();
        int count = (req.getCount() == null) ? 10 : req.getCount();
        String searchDttm = (req.getSearchDttm() == null || req.getSearchDttm().isBlank())
                ? LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"))
                : req.getSearchDttm();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("startX", String.valueOf(req.getStartX()));
        payload.put("startY", String.valueOf(req.getStartY()));
        payload.put("endX",   String.valueOf(req.getEndX()));
        payload.put("endY",   String.valueOf(req.getEndY()));
        payload.put("lang",   lang);
        payload.put("format", format);
        payload.put("count",  count);
        payload.put("searchDttm", searchDttm);

        String raw;
        try {
            String json = om.writeValueAsString(payload);
            RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("accept", "application/json")
                    .addHeader("content-type", "application/json")
                    .addHeader("appKey", props.getAppKey())
                    .build();

            try (Response res = http.newCall(request).execute()) {
                if (!res.isSuccessful()) {
                    String err = (res.body() != null) ? res.body().string() : "";
                    throw new IllegalStateException("SK Transit API error: HTTP " + res.code() + " - " + err);
                }
                raw = (res.body() != null) ? res.body().string() : "{}";
            }
        } catch (Exception e) {
            throw new RuntimeException("SK Transit API 호출 실패: " + e.getMessage(), e);
        }

        try {
            JsonNode root = om.readTree(raw);

            // 1) Tmap 스타일: result.path[]
            if (root.path("result").has("path")) {
                List<BusRouteDto> busRoutes = extractFromTmapPath(root.path("result").path("path"));
                return BusRouteResponse.builder()
                        .routes(busRoutes)
                        .source("tmap-path")
                        .rawSample(crop(raw))
                        .build();
            }

            // 2) 일반 스타일: metaData.plan.itineraries[]
            if (root.path("metaData").path("plan").has("itineraries")) {
                List<BusRouteDto> busRoutes = extractFromItineraries(root.path("metaData").path("plan").path("itineraries"));
                return BusRouteResponse.builder()
                        .routes(busRoutes)
                        .source("plan-itineraries")
                        .rawSample(crop(raw))
                        .build();
            }

            // 3) 알 수 없는 구조 → 전체에서 모드=BUS 흔적 탐색 (fallback)
            List<BusRouteDto> fallback = extractHeuristically(root);
            return BusRouteResponse.builder()
                    .routes(fallback)
                    .source("heuristic")
                    .rawSample(crop(raw))
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("SK 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    // --- 파서들 ---

    // Tmap: result.path[].pathType == 2 가 버스, 또는 legs[].mode == BUS
    private List<BusRouteDto> extractFromTmapPath(JsonNode pathArr) {
        if (!pathArr.isArray()) return List.of();
        List<BusRouteDto> out = new ArrayList<>();

        for (JsonNode p : pathArr) {
            int pathType = p.path("pathType").asInt(-1); // 2=BUS(가정)
            JsonNode info = p.path("info");
            JsonNode subPath = p.path("subPath"); // 각 구간

            // 버스 경로만
            if (pathType == 2 || containsBusMode(subPath)) {
                BusRouteDto dto = BusRouteDto.builder()
                        .totalTimeMin(info.path("totalTime").asInt(0))
                        .fare(info.path("payment").asInt(0))
                        .transferCount(info.path("transferCount").asInt(0))
                        .legs(extractBusLegsFromTmap(subPath))
                        .build();

                if (!dto.getLegs().isEmpty()) out.add(dto);
            }
        }
        return out;
    }

    private boolean containsBusMode(JsonNode subPath) {
        if (!subPath.isArray()) return false;
        for (JsonNode s : subPath) {
            String trafficType = s.path("trafficType").asText(""); // 버스면 "BUS" 또는 2 등 구조 다양
            String mode = s.path("mode").asText("");
            if ("BUS".equalsIgnoreCase(mode) || "BUS".equalsIgnoreCase(trafficType) || s.path("trafficType").asInt(-1) == 2) {
                return true;
            }
        }
        return false;
    }

    private List<BusLegDto> extractBusLegsFromTmap(JsonNode subPath) {
        if (!subPath.isArray()) return List.of();
        List<BusLegDto> legs = new ArrayList<>();
        for (JsonNode s : subPath) {
            boolean isBus = "BUS".equalsIgnoreCase(s.path("mode").asText(""))
                    || "BUS".equalsIgnoreCase(s.path("trafficType").asText(""))
                    || s.path("trafficType").asInt(-1) == 2;

            if (isBus) {
                String busName = s.path("lane").isArray() && s.path("lane").size() > 0
                        ? s.path("lane").get(0).path("name").asText("")
                        : s.path("busName").asText("");
                legs.add(BusLegDto.builder()
                        .busName(busName)
                        .agency(s.path("agency").asText(""))
                        .from(s.path("startName").asText(""))
                        .to(s.path("endName").asText(""))
                        .durationMin(s.path("sectionTime").asInt(0))
                        .stops(s.path("stationCount").asInt(0))
                        .build());
            }
        }
        return legs;
    }

    // 일반: metaData.plan.itineraries[].legs[].mode == BUS
    private List<BusRouteDto> extractFromItineraries(JsonNode itins) {
        if (!itins.isArray()) return List.of();
        List<BusRouteDto> out = new ArrayList<>();

        for (JsonNode itin : itins) {
            JsonNode legs = itin.path("legs");
            if (!legs.isArray()) continue;

            List<BusLegDto> busLegs = new ArrayList<>();
            for (JsonNode l : legs) {
                String mode = l.path("mode").asText("");
                if ("BUS".equalsIgnoreCase(mode)) {
                    busLegs.add(BusLegDto.builder()
                            .busName(firstNonEmpty(l.path("routeShortName").asText(""),
                                    l.path("route").asText(""),
                                    l.path("busName").asText("")))
                            .agency(l.path("agencyName").asText(""))
                            .from(l.path("from").path("name").asText(l.path("from").asText("")))
                            .to(l.path("to").path("name").asText(l.path("to").asText("")))
                            .durationMin(toMinutes(l.path("duration").asLong(0)))
                            .stops(l.path("numStops").asInt(0))
                            .build());
                }
            }

            if (!busLegs.isEmpty()) {
                out.add(BusRouteDto.builder()
                        .totalTimeMin(toMinutes(itin.path("duration").asLong(0)))
                        .fare(itin.path("fare").path("regular").path("amount").asInt(
                                itin.path("fare").path("fare").asInt(0)))
                        .transferCount(Math.max(0, legs.size() - 1))
                        .legs(busLegs)
                        .build());
            }
        }
        return out;
    }

    // 최후의 수단: 트리 전체를 훑어 'mode:BUS'가 있는 경로를 조합
    private List<BusRouteDto> extractHeuristically(JsonNode root) {
        // 단순/보수적: 트리 내 "itineraries"/"path" 후보만 탐색
        List<BusRouteDto> all = new ArrayList<>();
        if (root.has("itineraries")) all.addAll(extractFromItineraries(root.path("itineraries")));
        if (root.has("path")) all.addAll(extractFromTmapPath(root.path("path")));
        return all.stream().limit(5).collect(Collectors.toList());
    }

    private String firstNonEmpty(String... v) {
        for (String s : v) if (s != null && !s.isBlank()) return s;
        return "";
    }

    private int toMinutes(long secondsOrMinutes) {
        // 일부 응답은 초 단위, 일부는 분 단위일 수 있음.
        if (secondsOrMinutes > 0 && secondsOrMinutes < 1000) return (int) secondsOrMinutes;
        return (int) Math.round(secondsOrMinutes / 60.0);
    }

    private String crop(String raw) {
        if (raw == null) return "";
        return raw.substring(0, Math.min(raw.length(), 600)); // 디버깅용 샘플
    }
}
