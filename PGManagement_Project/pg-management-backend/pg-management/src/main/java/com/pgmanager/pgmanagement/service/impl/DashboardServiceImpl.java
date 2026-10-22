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
import com.pgmanager.pgmanagement.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final OwnerRepository ownerRepository;
    private final PgPropertyRepository pgPropertyRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final TenantAllocationRepository tenantAllocationRepository;
    private final RentRecordRepository rentRecordRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DashboardResponse> getMyDashboard(String ownerEmail) {
        Owner owner = getOwner(ownerEmail);
        List<PgProperty> pgs = pgPropertyRepository.findAllByOwner(owner);
        return buildDashboards(pgs);
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getPgDashboard(String ownerEmail, Long pgId) {
        Owner owner = getOwner(ownerEmail);
        PgProperty pg = pgPropertyRepository.findByIdAndOwner(pgId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PG not found"));
        return buildDashboards(List.of(pg)).get(0);
    }

    /**
     * Uses grouped aggregate queries for all PGs together. Query count remains constant as the
     * owner's number of PGs grows, and entities such as every bed/rent record are not loaded.
     */
    private List<DashboardResponse> buildDashboards(List<PgProperty> pgs) {
        if (pgs.isEmpty()) {
            return List.of();
        }

        List<Long> pgIds = pgs.stream().map(PgProperty::getId).toList();
        Map<Long, Long> roomsByPg = new HashMap<>();
        Map<Long, Long> activeTenantsByPg = new HashMap<>();
        Map<Long, long[]> bedsByPg = new HashMap<>(); // [total, occupied, available]
        Map<Long, DashboardRentStats> rentsByPg = new HashMap<>();

        for (DashboardRoomStats stats : roomRepository.findDashboardRoomCounts(pgIds)) {
            roomsByPg.put(stats.getPgId(), zero(stats.getTotalRooms()));
        }

        for (DashboardBedStats stats : bedRepository.findDashboardBedCounts(pgIds)) {
            long[] counts = bedsByPg.computeIfAbsent(stats.getPgId(), ignored -> new long[3]);
            if (stats.getStatus() == null || stats.getStatus() == BedStatus.INACTIVE) {
                continue;
            }
            long count = zero(stats.getBedCount());
            counts[0] += count;
            if (stats.getStatus() == BedStatus.OCCUPIED) {
                counts[1] += count;
            } else if (stats.getStatus() == BedStatus.AVAILABLE) {
                counts[2] += count;
            }
        }

        for (DashboardTenantStats stats : tenantAllocationRepository
                .findDashboardActiveTenantCounts(pgIds, AllocationStatus.ACTIVE)) {
            activeTenantsByPg.put(stats.getPgId(), zero(stats.getActiveTenants()));
        }

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        for (DashboardRentStats stats : rentRecordRepository.findDashboardRentTotals(
                pgIds, today, RentStatus.PENDING, RentStatus.OVERDUE, RentStatus.PAID)) {
            rentsByPg.put(stats.getPgId(), stats);
        }

        List<DashboardResponse> responses = new ArrayList<>(pgs.size());
        for (PgProperty pg : pgs) {
            long[] beds = bedsByPg.getOrDefault(pg.getId(), new long[3]);
            double occupancy = beds[0] == 0 ? 0.0 : roundToTwo((double) beds[1] * 100.0 / beds[0]);
            DashboardRentStats rent = rentsByPg.get(pg.getId());

            responses.add(DashboardResponse.builder()
                    .pgId(pg.getId())
                    .pgName(pg.getName())
                    .totalRooms(roomsByPg.getOrDefault(pg.getId(), 0L))
                    .totalBeds(beds[0])
                    .occupiedBeds(beds[1])
                    .availableBeds(beds[2])
                    .activeTenants(activeTenantsByPg.getOrDefault(pg.getId(), 0L))
                    .occupancyPercentage(occupancy)
                    .rentDue(rent == null ? 0.0 : money(rent.getRentDue()))
                    .rentOverdue(rent == null ? 0.0 : money(rent.getRentOverdue()))
                    .rentCollected(rent == null ? 0.0 : money(rent.getRentCollected()))
                    .build());
        }
        return responses;
    }

    private Owner getOwner(String email) {
        return ownerRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Owner not found"));
    }

    private long zero(Long value) {
        return value == null ? 0L : value;
    }

    private double money(Double value) {
        return value == null || !Double.isFinite(value) ? 0.0 : value;
    }

    private double roundToTwo(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
