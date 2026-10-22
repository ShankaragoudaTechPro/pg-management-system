package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.entity.RentRecord;
import com.pgmanager.pgmanagement.entity.RentStatus;
import com.pgmanager.pgmanagement.entity.Tenant;
import com.pgmanager.pgmanagement.repository.projection.DashboardRentStats;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RentRecordRepository
        extends JpaRepository<RentRecord, Long> {

    List<RentRecord> findAllByPg(PgProperty pg);

    @Query("""
        select r.pg.id as pgId,
               coalesce(sum(case when r.status = :pending and r.dueDate >= :today then r.rentAmount else 0.0 end), 0.0) as rentDue,
               coalesce(sum(case when r.status = :overdue or (r.status = :pending and r.dueDate < :today) then r.rentAmount else 0.0 end), 0.0) as rentOverdue,
               coalesce(sum(case when r.status = :paid then r.rentAmount else 0.0 end), 0.0) as rentCollected
        from RentRecord r
        where r.pg.id in :pgIds
        group by r.pg.id
        """)
    List<DashboardRentStats> findDashboardRentTotals(
            @Param("pgIds") java.util.Collection<Long> pgIds,
            @Param("today") LocalDate today,
            @Param("pending") RentStatus pending,
            @Param("overdue") RentStatus overdue,
            @Param("paid") RentStatus paid);

    List<RentRecord> findAllByTenant(Tenant tenant);

    Optional<RentRecord> findByIdAndPg(
            Long id,
            PgProperty pg
    );

    boolean existsByTenantAndPeriodStartAndPeriodEnd(
            Tenant tenant,
            LocalDate periodStart,
            LocalDate periodEnd
    );

    List<RentRecord> findAllByStatusAndDueDateBefore(
            RentStatus status,
            LocalDate date
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update RentRecord r
        set r.status = :overdue
        where r.status = :pending and r.dueDate < :today
        """)
    int markPendingRentOverdue(
            @Param("today") LocalDate today,
            @Param("pending") RentStatus pending,
            @Param("overdue") RentStatus overdue);

    // =========================================================
    // OWNER-SCOPED PAGINATION
    // =========================================================

    @Query("""
        SELECT r
        FROM RentRecord r
        JOIN FETCH r.tenant t
        JOIN FETCH r.pg p
        WHERE t.owner = :owner
        """)
    Page<RentRecord> findAllByOwner(
            @Param("owner") Owner owner,
            Pageable pageable
    );

    // =========================================================
    // OWNER + SEARCH
    // Search by tenant name or mobile
    // =========================================================

    @Query("""
        SELECT r
        FROM RentRecord r
        JOIN FETCH r.tenant t
        JOIN FETCH r.pg p
        WHERE t.owner = :owner
        AND (
            LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR t.mobile LIKE CONCAT('%', :search, '%')
        )
        """)
    Page<RentRecord> searchByOwner(
            @Param("owner") Owner owner,
            @Param("search") String search,
            Pageable pageable
    );

    // =========================================================
    // OWNER + STATUS + PG FILTER
    // =========================================================

    @Query("""
        SELECT r
        FROM RentRecord r
        JOIN FETCH r.tenant t
        JOIN FETCH r.pg p
        WHERE t.owner = :owner
        AND (:status IS NULL OR r.status = :status)
        AND (:pgId IS NULL OR p.id = :pgId)
        """)
    Page<RentRecord> filterByOwner(
            @Param("owner") Owner owner,
            @Param("status") RentStatus status,
            @Param("pgId") Long pgId,
            Pageable pageable
    );
}