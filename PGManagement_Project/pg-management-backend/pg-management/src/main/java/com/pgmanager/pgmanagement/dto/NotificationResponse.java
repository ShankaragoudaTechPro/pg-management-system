package com.pgmanager.pgmanagement.dto;

import com.pgmanager.pgmanagement.entity.NotificationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;

    private Long tenantId;

    private String tenantName;

    private Long rentId;

    private Long pgId;

    private String pgName;

    private NotificationType type;

    private String title;

    private String message;

    private boolean read;

    private LocalDateTime createdAt;
}