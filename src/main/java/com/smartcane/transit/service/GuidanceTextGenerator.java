package com.smartcane.transit.service;

import com.smartcane.transit.dto.response.ArrivalCheckResponse;
import com.smartcane.transit.dto.response.SkTransitRootDto;
import org.springframework.stereotype.Component;

/**
 * 시각장애인용 TTS 문구 생성기 (SK 응답 DTO 기반)
 *
 * - WALK : 남은 거리 위주로 안내
 * - BUS / SUBWAY : 이번 leg가 대중교통 구간인지에 따라 간단한 상태 안내
 *   (상세 "몇 정거장 남음"은 추후 ItineraryDto / TripState 확장 시 추가)
 */
@Component
public class GuidanceTextGenerator {

    public String from(ArrivalCheckResponse arrival,
                       TripState state,
                       SkTransitRootDto.ItineraryDto itinerary,
                       SkTransitRootDto.LegDto currentLeg) {

        double remain = Math.max(0, arrival.remainingMeters());
        String rawMode = (currentLeg != null && currentLeg.mode() != null)
                ? currentLeg.mode()
                : "WALK";
        String mode = rawMode.toUpperCase(); // "WALK" / "BUS" / "SUBWAY" 등

        String phase = (state != null && state.getPhase() != null)
                ? state.getPhase()
                : "";

        // 1) 이 leg/step 도착 처리
        if (arrival.arrived()) {
            return switch (phase) {
                case "ONBOARD" ->
                        "하차 지점에 도착했습니다. 천천히 내리신 후 다음 안내를 기다려 주세요.";
                case "TRANSFER" ->
                        "환승 지점에 도착했습니다. 다음 노선으로 이동해 주세요.";
                case "ARRIVED" ->
                        "최종 목적지에 도착했습니다.";
                default ->
                        "도착했습니다. 다음 구간으로 이동해 주세요.";
            };
        }

        // 2) 보행 구간 안내
        if ("WALK".equals(mode)) {
            if (remain <= 10) {
                return "곧 목적 지점입니다. 발을 천천히 옮기며 주변을 확인해 주세요.";
            }
            if (remain <= 30) {
                return String.format("앞으로 약 %.0f미터 남았습니다. 속도를 줄이고 주변을 확인해 주세요.", remain);
            }
            if (remain <= 80) {
                return String.format("앞으로 약 %.0f미터 직진 후, 다음 안내를 기다려 주세요.", remain);
            }
            return String.format("다음 안내까지 약 %.0f미터 이동해 주세요.", remain);
        }

        // 3) 버스 구간 안내
        if ("BUS".equals(mode)) {
            return switch (phase) {
                case "ONBOARD" ->
                        "버스에 탑승 중입니다. 하차 정류장 근처에서 다시 안내해 드리겠습니다.";
                case "TRANSFER" ->
                        "환승 버스를 기다리는 구간입니다. 정류장 근처에서 버스를 기다려 주세요.";
                default ->
                        "버스 구간입니다. 정류장에서 버스를 기다려 주세요.";
            };
        }

        // 4) 지하철 구간 안내
        if ("SUBWAY".equals(mode)) {
            return switch (phase) {
                case "ONBOARD" ->
                        "지하철에 탑승 중입니다. 하차역 근처에서 다시 안내해 드리겠습니다.";
                case "TRANSFER" ->
                        "환승역 구간입니다. 승강장과 환승 안내 표지판을 따라 이동해 주세요.";
                default ->
                        "지하철 구간입니다. 승강장으로 이동하여 열차를 기다려 주세요.";
            };
        }

        // 5) 그 외(확장용: 택시, KTX 등 들어올 때 커버)
        return "경로를 따라 이동해 주세요.";
    }

}
