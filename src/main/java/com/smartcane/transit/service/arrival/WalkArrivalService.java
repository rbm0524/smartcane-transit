package com.smartcane.transit.service.arrival;

import com.smartcane.transit.dto.request.ArrivalCheckRequest;
import com.smartcane.transit.dto.response.ArrivalCheckResponse;
import com.smartcane.transit.dto.response.SkTransitRootDto;
import com.smartcane.transit.util.GeoUtils;
import com.smartcane.transit.util.PolylineSnapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 보행(WALK) 구간의 진행/도착 판정을 담당하는 서비스.
 *
 * - 기본 판정 기준:
 *   · leg.steps[stepIndex].linestring 이 있으면 그 polyline 으로 도착 판정
 *   · 없으면 leg.passShape.linestring 으로 fallback
 *
 * - 기능:
 *   · 현재 step 의 안내 문구(description)를 currentInstruction 으로 제공
 *   · 도착 시 nextStepIndex/nextLegIndex 를 계산하여 다음 안내로 전진
 *   · lookAheadM 이 설정된 경우, 아직 도착 전이어도
 *     일정 거리 이내에 들어오면 다음 step 안내문을 미리 nextInstruction 로 내려준다.
 *
 * - 정류장 개념이 없으므로 currentStationIndex / stopsLeft 는 항상 null.
 */
@Service
public class WalkArrivalService {

    /** 안전한 리스트 접근용 유틸 */
    private static <T> T safeGet(List<T> list, int idx) {
        if (list == null || idx < 0 || idx >= list.size()) return null;
        return list.get(idx);
    }

    /** 에러/데이터 없음 상황에서 사용하는 공통 응답 */
    private static ArrivalCheckResponse notFound() {
        return new ArrivalCheckResponse(
                false,
                Double.NaN,
                "경로를 찾을 수 없습니다.",
                null,
                null,
                null,
                null,
                null
        );
    }

    /**
     * 보행(WALK) 구간에 대한 도착 판정 및 다음 안내 계산.
     *
     * @param itin SK ItineraryDto
     * @param req  ArrivalCheckRequest
     * @return ArrivalCheckResponse
     */
    public ArrivalCheckResponse evaluate(SkTransitRootDto.ItineraryDto itin, ArrivalCheckRequest req) {
        // 1) 현재 진행 중인 leg 추출
        var leg = safeGet(itin.legs(), req.legIndex());
        if (leg == null) return notFound();

        String currentInst = null; // 현재 step 의 안내 문구
        String line = null;        // 판정에 사용할 polyline (WKT-like "lon lat, ...")

        // 2) 우선 step.linestring 사용 (step 단위 보행 안내)
        if (leg.steps() != null && req.stepIndex() != null) {
            var step = safeGet(leg.steps(), req.stepIndex());
            if (step != null) {
                currentInst = step.description();
                line = step.linestring();
            }
        }

        // 3) step 에 라인스트링이 없으면 leg.passShape.linestring 으로 fallback
        if (line == null || line.isBlank()) {
            if (leg.passShape() != null) {
                line = leg.passShape().linestring();
            }
        }

        // polyline 이 없거나 파싱 실패 시 보호
        var pts = GeoUtils.parseLineString(line);
        if (pts == null || pts.isEmpty()) {
            return notFound();
        }

        // 4) 전체 polyline 길이와 현재 위치를 스냅한 지점까지의 거리 계산
        double total = GeoUtils.polylineLength(pts);
        var snap = PolylineSnapper.snapToPolyline(req.currLat(), req.currLon(), pts);

        double remaining = Math.max(0, total - snap.snappedMetersFromStart);
        boolean arrived = remaining <= req.arriveRadiusM();

        // 5) 다음 안내(스텝 인덱스 / 레그 인덱스) 계산
        String nextInstruction = null;
        Integer nextLegIndex = null;
        Integer nextStepIndex = null;

        if (arrived) {
            // 도착: 다음 스텝으로 전진, 마지막 스텝이면 다음 레그로 전이
            if (leg.steps() != null && req.stepIndex() != null) {
                int ni = req.stepIndex() + 1;
                if (ni < leg.steps().size()) {
                    nextStepIndex = ni;
                    nextInstruction = leg.steps().get(ni).description();
                } else {
                    nextLegIndex = req.legIndex() + 1;
                }
            } else {
                // stepIndex 가 없으면 해당 leg 전체를 하나의 구간으로 보고 바로 다음 leg 로
                nextLegIndex = req.legIndex() + 1;
            }
        } else if (req.lookAheadM() != null && remaining <= req.lookAheadM()) {
            // 도착 직전(lookAheadM 이내)에 들어오면, 다음 스텝 안내를 미리 알려준다.
            if (leg.steps() != null && req.stepIndex() != null) {
                int ni = req.stepIndex() + 1;
                if (ni < leg.steps().size()) {
                    nextStepIndex = ni; // 프리뷰 할 step index
                    nextInstruction = leg.steps().get(ni).description();
                }
            }
        }

        // 6) 현재 안내 문구 기본값
        if (currentInst == null || currentInst.isBlank()) {
            currentInst = "직진하세요.";
        }

        // 7) 최종 응답 빌드
        return new ArrivalCheckResponse(
                arrived,
                remaining,
                currentInst,
                nextInstruction,
                nextLegIndex,
                nextStepIndex,
                null,   // currentStationIndex (보행 구간은 정류장 개념 없음)
                null    // stopsLeft
        );
    }
}
