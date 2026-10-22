package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.NotificationResponse;
import com.pgmanager.pgmanagement.entity.*;
import com.pgmanager.pgmanagement.repository.NotificationRepository;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl
        implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final OwnerRepository ownerRepository;

    // =========================================================
    // GET ALL NOTIFICATIONS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getMyNotifications(
            String ownerEmail,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        return notificationRepository
                .findAllByOwner(owner, pageable)
                .map(this::mapToResponse);
    }

    // =========================================================
    // READ / UNREAD FILTER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotificationsByReadStatus(
            String ownerEmail,
            Boolean read,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        return notificationRepository
                .findByOwnerAndRead(
                        owner,
                        read,
                        pageable
                )
                .map(this::mapToResponse);
    }

    // =========================================================
    // UNREAD - EXISTING API
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        return notificationRepository
                .findAllByOwnerAndReadFalseOrderByCreatedAtDesc(
                        owner
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // MARK ONE AS READ
    // =========================================================

    @Override
    @Transactional
    public NotificationResponse markAsRead(
            String ownerEmail,
            Long notificationId) {

        Owner owner = getOwner(ownerEmail);

        Notification notification =
                notificationRepository
                        .findByIdAndOwner(
                                notificationId,
                                owner
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        notification.setRead(true);

        return mapToResponse(notification);
    }

    // =========================================================
    // MARK ALL AS READ
    // =========================================================

    @Override
    @Transactional
    public int markAllAsRead(
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        List<Notification> notifications =
                notificationRepository
                        .findAllByOwnerAndReadFalseOrderByCreatedAtDesc(
                                owner
                        );

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);

        return notifications.size();
    }

    // =========================================================
    // DELETE NOTIFICATION
    // =========================================================

    @Override
    @Transactional
    public void deleteNotification(
            String ownerEmail,
            Long notificationId) {

        Owner owner = getOwner(ownerEmail);

        Notification notification =
                notificationRepository
                        .findByIdAndOwner(
                                notificationId,
                                owner
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                )
                        );

        notificationRepository.delete(notification);
    }

    // =========================================================
    // GENERATE RENT NOTIFICATIONS
    // =========================================================

    @Override
    @Transactional
    public void generateRentNotifications() {

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        List<RentRecord> rents =
                notificationRepository
                        .findRentsForNotifications(
                                today,
                                tomorrow
                        );

        for (RentRecord rent : rents) {

            if (rent.getStatus() == RentStatus.PAID) {
                continue;
            }

            Tenant tenant = rent.getTenant();
            Owner owner = tenant.getOwner();

            // -------------------------------------------------
            // RENT DUE TOMORROW
            // -------------------------------------------------

            if (rent.getDueDate()
                    .equals(tomorrow)) {

                createNotification(
                        owner,
                        tenant,
                        rent,
                        NotificationType.RENT_DUE_TOMORROW,
                        "Rent Due Tomorrow",
                        tenant.getName()
                                + "'s rent of ₹"
                                + rent.getRentAmount()
                                + " is due tomorrow."
                );
            }

            // -------------------------------------------------
            // RENT DUE TODAY
            // -------------------------------------------------

            if (rent.getDueDate()
                    .equals(today)) {

                createNotification(
                        owner,
                        tenant,
                        rent,
                        NotificationType.RENT_DUE_TODAY,
                        "Rent Due Today",
                        tenant.getName()
                                + "'s rent of ₹"
                                + rent.getRentAmount()
                                + " is due today."
                );
            }

            // -------------------------------------------------
            // RENT OVERDUE
            // -------------------------------------------------

            if (rent.getStatus() == RentStatus.OVERDUE) {

                createNotification(
                        owner,
                        tenant,
                        rent,
                        NotificationType.RENT_OVERDUE,
                        "Rent Overdue",
                        tenant.getName()
                                + "'s rent of ₹"
                                + rent.getRentAmount()
                                + " is overdue."
                );
            }
        }
    }

    // =========================================================
    // CREATE NOTIFICATION
    // =========================================================

    private void createNotification(
            Owner owner,
            Tenant tenant,
            RentRecord rent,
            NotificationType type,
            String title,
            String message) {

        boolean alreadyExists =
                notificationRepository
                        .existsByOwnerAndRentAndType(
                                owner,
                                rent,
                                type
                        );

        if (alreadyExists) {
            return;
        }

        Notification notification =
                Notification.builder()
                        .owner(owner)
                        .tenant(tenant)
                        .rent(rent)
                        .type(type)
                        .title(title)
                        .message(message)
                        .read(false)
                        .build();

        notificationRepository.save(notification);
    }

    // =========================================================
    // OWNER
    // =========================================================

    private Owner getOwner(String email) {

        return ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Owner not found"
                        )
                );
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private NotificationResponse mapToResponse(
            Notification notification) {

        RentRecord rent =
                notification.getRent();

        return NotificationResponse.builder()
                .id(notification.getId())

                .tenantId(
                        notification
                                .getTenant()
                                .getId()
                )

                .tenantName(
                        notification
                                .getTenant()
                                .getName()
                )

                .rentId(
                        rent.getId()
                )

                .pgId(
                        rent.getPg().getId()
                )

                .pgName(
                        rent.getPg().getName()
                )

                .type(
                        notification.getType()
                )

                .title(
                        notification.getTitle()
                )

                .message(
                        notification.getMessage()
                )

                .read(
                        notification.isRead()
                )

                .createdAt(
                        notification.getCreatedAt()
                )

                .build();
    }
}