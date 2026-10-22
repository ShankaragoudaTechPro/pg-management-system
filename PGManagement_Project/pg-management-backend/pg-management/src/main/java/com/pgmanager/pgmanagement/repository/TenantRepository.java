package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.Tenant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

    List<Tenant> findAllByOwner(Owner owner);

    Optional<Tenant> findByIdAndOwner(Long id, Owner owner);

    boolean existsByMobileAndOwner(String mobile, Owner owner);

    @Query("""
        SELECT t
        FROM Tenant t
        WHERE t.owner = :owner
        AND (
            LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR t.mobile LIKE CONCAT('%', :search, '%')
            OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    List<Tenant> searchTenants(
            @Param("owner") Owner owner,
            @Param("search") String search
    );

    Page<Tenant> findAllByOwner(
            Owner owner,
            Pageable pageable
    );

    @Query("""
        SELECT t
        FROM Tenant t
        WHERE t.owner = :owner
        AND (
            LOWER(t.name) LIKE LOWER(CONCAT('%', :search, '%'))
            OR t.mobile LIKE CONCAT('%', :search, '%')
            OR LOWER(t.email) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        """)
    Page<Tenant> searchTenants(
            @Param("owner") Owner owner,
            @Param("search") String search,
            Pageable pageable
    );
    @Query("""
    SELECT t
    FROM Tenant t
    WHERE t.owner = :owner
    AND (:active IS NULL OR t.active = :active)
    AND (
        :pgId IS NULL
        OR EXISTS (
            SELECT a.id
            FROM TenantAllocation a
            WHERE a.tenant = t
            AND a.pg.id = :pgId
            AND a.status = com.pgmanager.pgmanagement.entity.AllocationStatus.ACTIVE
        )
    )
    """)
    Page<Tenant> filterTenants(
            @Param("owner") Owner owner,
            @Param("active") Boolean active,
            @Param("pgId") Long pgId,
            Pageable pageable
    );
}