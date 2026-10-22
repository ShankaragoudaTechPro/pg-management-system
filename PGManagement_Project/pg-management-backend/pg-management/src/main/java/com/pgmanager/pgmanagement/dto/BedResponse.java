package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.BedStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BedResponse {

    private Long id;

    private Long roomId;

    private Integer bedNumber;

    private BedStatus status;
}