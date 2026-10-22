package com.pgmanager.pgmanagement.dto.pg;

import com.pgmanager.pgmanagement.entity.PgStatus;
import com.pgmanager.pgmanagement.entity.PgType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PgPropertyResponse {

    private Long id;

    private String name;

    private PgType type;

    private String description;

    private PgStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}