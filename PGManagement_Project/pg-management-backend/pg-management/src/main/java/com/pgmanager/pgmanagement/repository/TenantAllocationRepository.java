package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.AllocationStatus;
import com.pgmanager.pgmanagement.entity.Tenant;
import com.pgmanager.pgmanagement.entity.TenantAllocation;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.entity.Bed;
import com.pgmanager.pgmanagement.repository.projection.DashboardTenantStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TenantAllocationRepository
        extends JpaRepository<TenantAllocation, Long> {

    Optional<TenantAllocation> findByTenantAndStatus(
            Tenant tenant,
            AllocationStatus status
    );

    List<TenantAllocation> findAllByTenant(
            Tenant tenant
    );

    List<TenantAllocation> findAllByPg(
            PgProperty pg
    );

    @Query("""
        select a.pg.id as pgId, count(distinct a.tenant.id) as activeTenants
        from TenantAllocation a
        where a.pg.id in :pgIds
          and a.status = :activeStatus
          and a.tenant.active = true
          and a.tenant.owner.id = a.pg.owner.id
        group by a.pg.id
        """)
    List<DashboardTenantStats> findDashboardActiveTenantCounts(
            @Param("pgIds") Collection<Long> pgIds,
            @Param("activeStatus") AllocationStatus activeStatus);

    boolean existsByBedAndStatus(
            Bed bed,
            AllocationStatus status
    );
}