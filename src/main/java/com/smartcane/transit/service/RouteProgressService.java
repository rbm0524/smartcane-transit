package com.smartcane.transit.service;

import com.smartcane.transit.dto.request.ArrivalCheckRequest;
import com.smartcane.transit.dto.request.ProgressUpdateEnvelope;
import com.smartcane.transit.dto.response.ArrivalCheckResponse;
import com.smartcane.transit.dto.response.GuidanceResponse;
import com.smartcane.transit.dto.response.SkTransitRootDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 경로 진행 상황 처리 서비스
 *
 * - updateProgress : iOS에서 주기적으로 보내는 진행 상황(위치/센서)을 기반으로
 *                    ProgressCoordinator에 위임하여 도착 체크 + 안내문(TTS) 생성.
 * - checkWalkStep / checkTransitLeg : (선택) 레거시 도착 체크용 헬퍼.
 *   컨트롤러에서 _legacy 엔드포인트를 완전히 막는다면 제거해도 무방하다.
 */
@Service
@RequiredArgsConstructor
public class RouteProgressService {

    private final ProgressCoordinator coordinator;

    /**
     * (선택) 레거시: 보행 구간 도착 여부만 단건 체크
     */
    @Deprecated
    public ArrivalCheckResponse checkWalkStep(SkTransitRootDto.ItineraryDto itinerary,
                                              ArrivalCheckRequest req) {
        return coordinator.checkWalkStep(itinerary, req);
    }

    /**
     * (선택) 레거시: 버스/지하철 구간 도착 여부만 단건 체크
     */
    @Deprecated
    public ArrivalCheckResponse checkTransitLeg(SkTransitRootDto.ItineraryDto itinerary,
                                                ArrivalCheckRequest req) {
        return coordinator.checkTransitLeg(itinerary, req);
    }

    /**
     * 신규 플로우:
     *  - tripId 기준으로 TripState/MetaData를 조회
     *  - 현재 위치/센서 정보를 받아서
     *    · 어떤 itinerary/leg/step 에 있는지 판단
     *    · ArrivalCheckResponse 계산
     *    · GuidanceTextGenerator로 TTS 문구 생성
     *  - 최종 GuidanceResponse 반환
     */
    public GuidanceResponse updateProgress(String tripId, ProgressUpdateEnvelope envelope) {
        return coordinator.updateProgress(tripId, envelope);
    }
}