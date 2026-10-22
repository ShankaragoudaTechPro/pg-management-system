package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.NotificationResponse;
import com.pgmanager.pgmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // =========================================================
    // GET NOTIFICATIONS
    // =========================================================

    @GetMapping
    public ResponseEntity<?> getNotifications(
            Authentication authentication,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) Boolean read) {

        String ownerEmail = authentication.getName();
        boolean paginationRequested = page != null || size != null || read != null;

        // Existing Angular notification UI expects an array without query parameters.
        if (!paginationRequested) {
            Page<NotificationResponse> all = notificationService.getMyNotifications(
                    ownerEmail,
                    PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "createdAt"))
            );
            return ResponseEntity.ok(all.getContent());
        }

        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 10 : size;
        validatePagination(pageNumber, pageSize);

        Pageable pageable = PageRequest.of(
                pageNumber,
                pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<NotificationResponse> response = read == null
                ? notificationService.getMyNotifications(ownerEmail, pageable)
                : notificationService.getNotificationsByReadStatus(ownerEmail, read, pageable);

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // UNREAD - BACKWARD COMPATIBILITY
    // =========================================================

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            Authentication authentication) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok(
                notificationService
                        .getUnreadNotifications(
                                ownerEmail
                        )
        );
    }

    // =========================================================
    // MARK ONE AS READ
    // =========================================================

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
            Authentication authentication,
            @PathVariable Long notificationId) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok(
                notificationService.markAsRead(
                        ownerEmail,
                        notificationId
                )
        );
    }

    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    @PatchMapping("/read-all")
    public ResponseEntity<String> markAllAsRead(
            Authentication authentication) {

        String ownerEmail =
                authentication.getName();

        int count =
                notificationService.markAllAsRead(
                        ownerEmail
                );

        return ResponseEntity.ok(
                count + " notification(s) marked as read"
        );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @DeleteMapping("/{notificationId}")
    public ResponseEntity<String> deleteNotification(
            Authentication authentication,
            @PathVariable Long notificationId) {

        String ownerEmail =
                authentication.getName();

        notificationService.deleteNotification(
                ownerEmail,
                notificationId
        );

        return ResponseEntity.ok(
                "Notification deleted successfully"
        );
    }

    // =========================================================
    // PAGINATION VALIDATION
    // =========================================================

    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {
            throw new RuntimeException(
                    "Page must be greater than or equal to 0"
            );
        }

        if (size < 1 || size > 100) {
            throw new RuntimeException(
                    "Size must be between 1 and 100"
            );
        }
    }
}