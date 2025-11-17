package com.smartcane.transit.dto.request;

import com.smartcane.transit.dto.response.SkTransitRootDto;

public record ProgressUpdateEnvelope(
        SkTransitRootDto.MetaDataDto metaData,
        ProgressUpdateRequest progress
) {}
