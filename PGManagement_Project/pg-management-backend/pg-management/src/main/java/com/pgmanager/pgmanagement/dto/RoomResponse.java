package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.BedStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RoomResponse {

    private Long id;

    private Long pgId;

    private String roomNumber;

    private Integer sharing;

    private Integer floor;

    private boolean active;

    private List<BedResponse> beds;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}