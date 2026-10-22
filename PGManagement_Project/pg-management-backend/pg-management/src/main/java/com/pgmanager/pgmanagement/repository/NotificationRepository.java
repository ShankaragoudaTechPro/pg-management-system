package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Notification;
import com.pgmanager.pgmanagement.entity.NotificationType;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.RentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // =========================================================
    // PAGINATION
    // =========================================================

    @Query("""
        SELECT n
        FROM Notification n
        JOIN FETCH n.tenant t
        JOIN FETCH n.rent r
        JOIN FETCH r.pg p
        WHERE n.owner = :owner
        """)
    Page<Notification> findAllByOwner(
            @Param("owner") Owner owner,
            Pageable pageable
    );

    // =========================================================
    // READ / UNREAD FILTER
    // =========================================================

    @Query("""
        SELECT n
        FROM Notification n
        JOIN FETCH n.tenant t
        JOIN FETCH n.rent r
        JOIN FETCH r.pg p
        WHERE n.owner = :owner
        AND (:read IS NULL OR n.read = :read)
        """)
    Page<Notification> findByOwnerAndRead(
            @Param("owner") Owner owner,
            @Param("read") Boolean read,
            Pageable pageable
    );

    // =========================================================
    // EXISTING UNREAD API
    // =========================================================

    List<Notification>
    findAllByOwnerAndReadFalseOrderByCreatedAtDesc(
            Owner owner
    );

    // =========================================================
    // OWNER-SCOPED SINGLE NOTIFICATION
    // =========================================================

    Optional<Notification> findByIdAndOwner(
            Long notificationId,
            Owner owner
    );

    // =========================================================
    // DUPLICATE CHECK
    // =========================================================

    boolean existsByOwnerAndRentAndType(
            Owner owner,
            RentRecord rent,
            NotificationType type
    );

    // =========================================================
    // RENT NOTIFICATION GENERATION
    // =========================================================

    @Query("""
        SELECT r
        FROM RentRecord r
        JOIN FETCH r.tenant t
        JOIN FETCH t.owner o
        JOIN FETCH r.pg p
        WHERE r.status <> com.pgmanager.pgmanagement.entity.RentStatus.PAID
        AND (
            r.dueDate = :today
            OR r.dueDate = :tomorrow
            OR r.status = com.pgmanager.pgmanagement.entity.RentStatus.OVERDUE
        )
        """)
    List<RentRecord> findRentsForNotifications(
            @Param("today") LocalDate today,
            @Param("tomorrow") LocalDate tomorrow
    );
}