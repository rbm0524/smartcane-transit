package com.smartcane.transit.service;

import com.smartcane.transit.config.GuidanceProperties;
import com.smartcane.transit.dto.request.ArrivalCheckRequest;
import com.smartcane.transit.dto.request.ProgressUpdateEnvelope;
import com.smartcane.transit.dto.request.ProgressUpdateRequest;
import com.smartcane.transit.dto.response.ArrivalCheckResponse;
import com.smartcane.transit.dto.response.GuidanceResponse;
import com.smartcane.transit.dto.response.SkTransitRootDto;
import com.smartcane.transit.service.arrival.TransitArrivalService;
import com.smartcane.transit.service.arrival.WalkArrivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 진행 업데이트의 오케스트레이션 레이어.
 * - TripState 로드/초기화/저장
 * - 현재 Leg의 모드에 따라 적절한 도착판정 서비스(Walk/Transit) 호출
 * - ArrivalCheckResponse를 기반으로 상태 전이 및 TTS 생성
 */
@Service
@RequiredArgsConstructor
public class ProgressCoordinator {

    private final TripStore tripStore;
    private final GuidanceTextGenerator guidanceTextGenerator;
    private final WalkArrivalService walkArrivalService;
    private final TransitArrivalService transitArrivalService;
    private final GuidanceProperties props;

    /** 보행 구간 판정(테스트/디버깅용 공개) */
    public ArrivalCheckResponse checkWalkStep(SkTransitRootDto.ItineraryDto itin,
                                              ArrivalCheckRequest req) {
        return walkArrivalService.evaluate(itin, req);
    }

    /** 대중교통 구간 판정(테스트/디버깅용 공개) */
    public ArrivalCheckResponse checkTransitLeg(SkTransitRootDto.ItineraryDto itin,
                                                ArrivalCheckRequest req) {
        return transitArrivalService.evaluate(itin, req);
    }

    // 중앙값 계산 유틸
    private static double median(java.util.Deque<Double> dq) {
        if (dq.isEmpty()) return Double.NaN;
        var arr = dq.stream().mapToDouble(Double::doubleValue).sorted().toArray();
        int n = arr.length;
        return (n % 2 == 1) ? arr[n / 2] : (arr[n / 2 - 1] + arr[n / 2]) / 2.0;
    }

    private static void pushWithCap(java.util.Deque<Double> dq, double v, int cap) {
        dq.addLast(v);
        while (dq.size() > cap) dq.removeFirst();
    }

    /**
     * iOS 진행 업링크 처리:
     * - Envelope(metaData, progress) 수신 → 상태 로드 → 도착판정 → 상태전이 → TTS → 응답
     */
    public GuidanceResponse updateProgress(String tripId, ProgressUpdateEnvelope envelope) {
        // metaData 타입: SkTransitRootDto.MetaDataDto
        SkTransitRootDto.MetaDataDto meta = envelope.metaData();
        ProgressUpdateRequest p = envelope.progress();

        // 1) 상태 로드/초기화
        TripState state = tripStore.load(tripId);
        if (state == null) {
            state = new TripState(tripId, 0, 0, null, "WALKING");
            tripStore.init(tripId, 0, 0, null, "WALKING");
        }

        // 2) 속도 게이팅: 너무 느리면(정지/튐) 샘플 반영을 보수적으로
        if (p.speedMps() != null && p.speedMps() < props.getMinSpeedMps()) {
            pushWithCap(state.getLatBuf(), p.lat(), props.getMedianWindow());
            pushWithCap(state.getLonBuf(), p.lon(), props.getMedianWindow());
        } else {
            pushWithCap(state.getLatBuf(), p.lat(), props.getMedianWindow());
            pushWithCap(state.getLonBuf(), p.lon(), props.getMedianWindow());
        }

        // 3) 중앙값 좌표로 판정 수행
        double latMed = median(state.getLatBuf());
        double lonMed = median(state.getLonBuf());
        if (Double.isNaN(latMed) || Double.isNaN(lonMed)) {
            latMed = p.lat();
            lonMed = p.lon();
        }

        // 4) 현재 Itinerary / Leg 인덱스 보정
        var itineraries = meta.plan().itineraries();
        if (state.getItineraryIndex() < 0 || state.getItineraryIndex() >= itineraries.size()) {
            state.setItineraryIndex(0);
        }
        SkTransitRootDto.ItineraryDto itinerary = itineraries.get(state.getItineraryIndex());

        if (state.getLegIndex() < 0 || state.getLegIndex() >= itinerary.legs().size()) {
            state.setLegIndex(0);
        }
        SkTransitRootDto.LegDto currentLeg = itinerary.legs().get(state.getLegIndex());

        // 5) 모드별 파라미터 선택 (문자열 기반: "WALK" / "BUS" / "SUBWAY")
        String modeRaw = currentLeg.mode() != null ? currentLeg.mode() : "WALK";
        String mode = modeRaw.toUpperCase();
        boolean isWalk = "WALK".equals(mode);

        double arriveRadius = isWalk ? props.getArriveRadiusWalkM() : props.getArriveRadiusTransitM();
        Double lookAhead = isWalk ? props.getLookAheadWalkM() : null;

        // 클라이언트가 보낸 값 우선
        if (p.arriveRadiusM() != null) {
            arriveRadius = p.arriveRadiusM();
        }
        if (isWalk && p.lookAheadM() != null) {
            lookAhead = p.lookAheadM();
        }

        // 6) ArrivalCheckRequest 생성 (중앙값 좌표 사용)
        ArrivalCheckRequest areq = new ArrivalCheckRequest(
                latMed, lonMed,
                state.getItineraryIndex(), state.getLegIndex(),
                state.getStepIndex(),
                arriveRadius,
                lookAhead
        );

        // 7) 도착 판정
        ArrivalCheckResponse ares = isWalk
                ? walkArrivalService.evaluate(itinerary, areq)
                : transitArrivalService.evaluate(itinerary, areq);

        // 8) 히스테리시스: 연속 N번 도착이어야 진짜 도착
        if (ares.arrived()) {
            state.setArrivalStreak(state.getArrivalStreak() + 1);
        } else {
            state.setArrivalStreak(0);
        }
        boolean arrivedStable = state.getArrivalStreak() >= props.getArrivalHysteresisN();

        Integer nextLeg = arrivedStable ? ares.nextLegIndex() : null;
        Integer nextStep = arrivedStable ? ares.nextStepIndex() : null;

        if (nextLeg != null) {
            int bounded = Math.min(nextLeg, Math.max(0, itinerary.legs().size() - 1));
            state.setLegIndex(bounded);
        }
        if (nextStep != null) {
            state.setStepIndex(nextStep);
        }

        // 9) phase 업데이트
        state.setPhase(isWalk ? "WALKING" : "ONBOARD");

        // 10) 최근 업링크 시각/좌표 업데이트
        long now = (p.timestampEpochMs() != null) ? p.timestampEpochMs() : System.currentTimeMillis();
        state.setLastLon(p.lon());
        state.setLastLat(p.lat());
        state.setLastTs(now);

        tripStore.save(tripId, state);

        // 11) 안내 문구 생성 (Sk DTO 기준 GuidanceTextGenerator)
        String tts = guidanceTextGenerator.from(ares, state, itinerary, currentLeg);

        return new GuidanceResponse(
                tripId,
                state.getItineraryIndex(),
                state.getLegIndex(),
                state.getPhase(),
                tts,
                Math.max(0, ares.remainingMeters()),
                null
        );
    }
}
