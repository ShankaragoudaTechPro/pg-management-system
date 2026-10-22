package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.DashboardResponse;
import com.pgmanager.pgmanagement.entity.AllocationStatus;
import com.pgmanager.pgmanagement.entity.BedStatus;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.entity.RentStatus;
import com.pgmanager.pgmanagement.repository.BedRepository;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.repository.PgPropertyRepository;
import com.pgmanager.pgmanagement.repository.RentRecordRepository;
import com.pgmanager.pgmanagement.repository.RoomRepository;
import com.pgmanager.pgmanagement.repository.TenantAllocationRepository;
import com.pgmanager.pgmanagement.repository.projection.DashboardBedStats;
import com.pgmanager.pgmanagement.repository.projection.DashboardRentStats;
import com.pgmanager.pgmanagement.repository.projection.DashboardRoomStats;
import com.pgmanager.pgmanagement.repository.projection.DashboardTenantStats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock private OwnerRepository ownerRepository;
    @Mock private PgPropertyRepository pgPropertyRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private BedRepository bedRepository;
    @Mock private TenantAllocationRepository tenantAllocationRepository;
    @Mock private RentRecordRepository rentRecordRepository;

    @InjectMocks private DashboardServiceImpl dashboardService;

    @Test
    void aggregatesDashboardStatisticsWithoutLoadingEntityCollections() {
        Owner owner = mock(Owner.class);
        PgProperty pg = PgProperty.builder().id(7L).name("Demo PG").build();
        when(ownerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(pgPropertyRepository.findAllByOwner(owner)).thenReturn(List.of(pg));

        DashboardRoomStats roomStats = mock(DashboardRoomStats.class);
        when(roomStats.getPgId()).thenReturn(7L);
        when(roomStats.getTotalRooms()).thenReturn(4L);
        when(roomRepository.findDashboardRoomCounts(anyCollection())).thenReturn(List.of(roomStats));

        DashboardBedStats available = bedStats(7L, BedStatus.AVAILABLE, 3L);
        DashboardBedStats occupied = bedStats(7L, BedStatus.OCCUPIED, 2L);
        DashboardBedStats inactive = bedStats(7L, BedStatus.INACTIVE, 5L);
        when(bedRepository.findDashboardBedCounts(anyCollection()))
                .thenReturn(List.of(available, occupied, inactive));

        DashboardTenantStats tenants = mock(DashboardTenantStats.class);
        when(tenants.getPgId()).thenReturn(7L);
        when(tenants.getActiveTenants()).thenReturn(2L);
        when(tenantAllocationRepository.findDashboardActiveTenantCounts(
                anyCollection(), eq(AllocationStatus.ACTIVE))).thenReturn(List.of(tenants));

        DashboardRentStats rent = mock(DashboardRentStats.class);
        when(rent.getPgId()).thenReturn(7L);
        when(rent.getRentDue()).thenReturn(10000.0);
        when(rent.getRentOverdue()).thenReturn(2000.0);
        when(rent.getRentCollected()).thenReturn(5000.0);
        when(rentRecordRepository.findDashboardRentTotals(
                anyCollection(), any(LocalDate.class), eq(RentStatus.PENDING),
                eq(RentStatus.OVERDUE), eq(RentStatus.PAID))).thenReturn(List.of(rent));

        List<DashboardResponse> result = dashboardService.getMyDashboard("owner@example.com");

        assertEquals(1, result.size());
        DashboardResponse response = result.get(0);
        assertEquals(7L, response.getPgId());
        assertEquals("Demo PG", response.getPgName());
        assertEquals(4L, response.getTotalRooms());
        assertEquals(5L, response.getTotalBeds());
        assertEquals(2L, response.getOccupiedBeds());
        assertEquals(3L, response.getAvailableBeds());
        assertEquals(2L, response.getActiveTenants());
        assertEquals(40.0, response.getOccupancyPercentage());
        assertEquals(10000.0, response.getRentDue());
        assertEquals(2000.0, response.getRentOverdue());
        assertEquals(5000.0, response.getRentCollected());

        verify(roomRepository).findDashboardRoomCounts(anyCollection());
        verify(bedRepository).findDashboardBedCounts(anyCollection());
        verify(tenantAllocationRepository).findDashboardActiveTenantCounts(
                anyCollection(), eq(AllocationStatus.ACTIVE));
    }

    @Test
    void returnsEmptyDashboardWhenOwnerHasNoProperties() {
        Owner owner = mock(Owner.class);
        when(ownerRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        when(pgPropertyRepository.findAllByOwner(owner)).thenReturn(List.of());

        assertEquals(List.of(), dashboardService.getMyDashboard("owner@example.com"));
    }

    private DashboardBedStats bedStats(Long pgId, BedStatus status, Long count) {
        DashboardBedStats stats = mock(DashboardBedStats.class);
        when(stats.getPgId()).thenReturn(pgId);
        when(stats.getStatus()).thenReturn(status);
        when(stats.getBedCount()).thenReturn(count);
        return stats;
    }
}
