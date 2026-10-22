package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.OwnerStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OwnerResponse {

    private Long id;
    private String name;
    private String mobile;
    private String email;
    private OwnerStatus status;
    private LocalDateTime createdAt;
}