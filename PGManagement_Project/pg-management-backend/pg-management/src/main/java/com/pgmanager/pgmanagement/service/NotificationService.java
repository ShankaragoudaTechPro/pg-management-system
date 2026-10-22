package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationService {

    Page<NotificationResponse> getMyNotifications(
            String ownerEmail,
            Pageable pageable
    );

    Page<NotificationResponse> getNotificationsByReadStatus(
            String ownerEmail,
            Boolean read,
            Pageable pageable
    );

    // Kept for compatibility
    List<NotificationResponse> getUnreadNotifications(
            String ownerEmail
    );

    NotificationResponse markAsRead(
            String ownerEmail,
            Long notificationId
    );

    int markAllAsRead(
            String ownerEmail
    );

    void deleteNotification(
            String ownerEmail,
            Long notificationId
    );

    void generateRentNotifications();
}