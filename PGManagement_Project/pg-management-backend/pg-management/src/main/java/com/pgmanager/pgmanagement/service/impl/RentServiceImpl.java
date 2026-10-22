package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.RentRequest;
import com.pgmanager.pgmanagement.dto.MarkRentPaidRequest;
import com.pgmanager.pgmanagement.dto.RentResponse;
import com.pgmanager.pgmanagement.entity.*;
import com.pgmanager.pgmanagement.repository.*;
import com.pgmanager.pgmanagement.service.RentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RentServiceImpl implements RentService {

    private final RentRecordRepository rentRecordRepository;
    private final TenantRepository tenantRepository;
    private final OwnerRepository ownerRepository;
    private final TenantAllocationRepository allocationRepository;

    // =========================================================
    // CREATE RENT
    // =========================================================

    @Override
    @Transactional
    public RentResponse createRent(
            String ownerEmail,
            Long tenantId,
            RentRequest request) {

        Owner owner = getOwner(ownerEmail);

        Tenant tenant = tenantRepository
                .findByIdAndOwner(
                        tenantId,
                        owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tenant not found"));

        if (!tenant.isActive()) {

            throw new RuntimeException(
                    "Cannot create rent for inactive tenant");
        }

        if (request.getPeriodEnd()
                .isBefore(request.getPeriodStart())) {

            throw new RuntimeException(
                    "Period end cannot be before period start");
        }

        if (request.getDueDate()
                .isBefore(request.getPeriodStart())) {

            throw new RuntimeException(
                    "Due date cannot be before period start");
        }

        if (request.getRentAmount() == null
                || request.getRentAmount() <= 0) {

            throw new RuntimeException(
                    "Rent amount must be greater than zero");
        }

        if (rentRecordRepository
                .existsByTenantAndPeriodStartAndPeriodEnd(
                        tenant,
                        request.getPeriodStart(),
                        request.getPeriodEnd())) {

            throw new RuntimeException(
                    "Rent record already exists for this period");
        }

        TenantAllocation allocation =
                allocationRepository
                        .findByTenantAndStatus(
                                tenant,
                                AllocationStatus.ACTIVE)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Active tenant allocation not found"));

        validateAllocationOwnership(
                allocation,
                owner);

        PgProperty pg = allocation.getPg();

        RentStatus status =
                request.getDueDate()
                        .isBefore(LocalDate.now())
                        ? RentStatus.OVERDUE
                        : RentStatus.PENDING;

        RentRecord rent = RentRecord.builder()
                .tenant(tenant)
                .pg(pg)
                .rentAmount(request.getRentAmount())
                .periodStart(request.getPeriodStart())
                .periodEnd(request.getPeriodEnd())
                .dueDate(request.getDueDate())
                .status(status)
                .paymentMode(null)
                .paymentNote(null)
                .build();

        RentRecord savedRent =
                rentRecordRepository.save(rent);

        return mapToResponse(savedRent);
    }

    // =========================================================
    // ALL RENTS - OLD API
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<RentResponse> getMyRents(
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        return rentRecordRepository
                .findAllByOwner(
                        owner,
                        Pageable.unpaged())
                .getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // PAGINATION
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<RentResponse> getMyRents(
            String ownerEmail,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        return rentRecordRepository
                .findAllByOwner(
                        owner,
                        pageable)
                .map(this::mapToResponse);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<RentResponse> searchRents(
            String ownerEmail,
            String search,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        if (search == null
                || search.trim().isEmpty()) {

            return getMyRents(
                    ownerEmail,
                    pageable);
        }

        return rentRecordRepository
                .searchByOwner(
                        owner,
                        search.trim(),
                        pageable)
                .map(this::mapToResponse);
    }

    // =========================================================
    // FILTER
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<RentResponse> filterRents(
            String ownerEmail,
            String status,
            Long pgId,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        RentStatus rentStatus = null;

        if (status != null
                && !status.trim().isEmpty()) {

            try {

                rentStatus =
                        RentStatus.valueOf(
                                status.trim()
                                        .toUpperCase());

            } catch (IllegalArgumentException ex) {

                throw new RuntimeException(
                        "Invalid rent status. Use PENDING, PAID or OVERDUE");
            }
        }

        return rentRecordRepository
                .filterByOwner(
                        owner,
                        rentStatus,
                        pgId,
                        pageable)
                .map(this::mapToResponse);
    }

    // =========================================================
    // TENANT RENT HISTORY
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<RentResponse> getTenantRents(
            String ownerEmail,
            Long tenantId) {

        Owner owner = getOwner(ownerEmail);

        Tenant tenant = tenantRepository
                .findByIdAndOwner(
                        tenantId,
                        owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Tenant not found"));

        return rentRecordRepository
                .findAllByTenant(tenant)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    // =========================================================
    // GET RENT BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public RentResponse getRentById(
            String ownerEmail,
            Long rentId) {

        Owner owner = getOwner(ownerEmail);

        RentRecord rent =
                rentRecordRepository
                        .findById(rentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Rent record not found"));

        validateTenantOwner(
                rent.getTenant(),
                owner);

        return mapToResponse(rent);
    }

    // =========================================================
    // MARK RENT AS PAID
    // =========================================================

    @Override
    @Transactional
    public RentResponse markRentAsPaid(
            String ownerEmail,
            Long rentId,
            MarkRentPaidRequest request) {

        Owner owner = getOwner(ownerEmail);

        RentRecord rent =
                rentRecordRepository
                        .findById(rentId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Rent record not found"));

        validateTenantOwner(
                rent.getTenant(),
                owner);

        if (rent.getStatus()
                == RentStatus.PAID) {

            throw new RuntimeException(
                    "Rent is already marked as paid");
        }

        if (request.getPaymentMode() == null) {

            throw new RuntimeException(
                    "Payment mode is required");
        }

        rent.setStatus(
                RentStatus.PAID);

        rent.setPaidDate(
                request.getPaidDate() == null
                        ? LocalDate.now()
                        : request.getPaidDate());

        rent.setPaymentMode(
                request.getPaymentMode());

        rent.setPaymentNote(
                request.getPaymentNote() == null
                        ? null
                        : request.getPaymentNote().trim());

        RentRecord updatedRent =
                rentRecordRepository.save(rent);

        return mapToResponse(updatedRent);
    }

    // =========================================================
    // UPDATE OVERDUE RENTS
    // =========================================================

    @Override
    @Transactional
    public void updateOverdueRents() {

        LocalDate today =
                LocalDate.now(ZoneId.of("Asia/Kolkata"));

        // One database update avoids loading and saving every overdue row individually.
        rentRecordRepository.markPendingRentOverdue(
                today,
                RentStatus.PENDING,
                RentStatus.OVERDUE);
    }

    // =========================================================
    // GENERATE MONTHLY RENTS
    // =========================================================

    @Override
    @Transactional
    public void generateMonthlyRents() {

        /*
         * Scheduler intentionally processes tenants
         * from all owners.
         *
         * This is NOT an owner-facing API.
         * Every generated rent remains attached to
         * the tenant's current allocation and PG.
         */

        List<Tenant> activeTenants =
                tenantRepository
                        .findAll()
                        .stream()
                        .filter(Tenant::isActive)
                        .toList();

        LocalDate today =
                LocalDate.now();

        for (Tenant tenant : activeTenants) {

            // Safety check: active tenant must belong to an owner
            if (tenant.getOwner() == null) {
                continue;
            }

            TenantAllocation allocation =
                    allocationRepository
                            .findByTenantAndStatus(
                                    tenant,
                                    AllocationStatus.ACTIVE)
                            .orElse(null);

            if (allocation == null) {
                continue;
            }

            // Safety check: allocation must belong to this tenant
            if (!allocation.getTenant()
                    .getId()
                    .equals(tenant.getId())) {

                continue;
            }

            // Safety check: allocation must belong
            // to the same owner as the tenant
            if (allocation.getPg() == null
                    || allocation.getPg()
                    .getOwner() == null
                    || !allocation.getPg()
                    .getOwner()
                    .getId()
                    .equals(
                            tenant.getOwner().getId())) {

                continue;
            }

            LocalDate cycleStart =
                    allocation.getStartDate();

            if (cycleStart == null) {
                continue;
            }

            while (!cycleStart.isAfter(today)) {

                LocalDate cycleEnd =
                        cycleStart
                                .plusMonths(1)
                                .minusDays(1);

                LocalDate dueDate =
                        cycleStart.plusMonths(1);

                boolean alreadyExists =
                        rentRecordRepository
                                .existsByTenantAndPeriodStartAndPeriodEnd(
                                        tenant,
                                        cycleStart,
                                        cycleEnd);

                if (!alreadyExists) {

                    RentStatus status =
                            dueDate.isBefore(today)
                                    ? RentStatus.OVERDUE
                                    : RentStatus.PENDING;

                    RentRecord rent =
                            RentRecord.builder()
                                    .tenant(tenant)
                                    .pg(allocation.getPg())
                                    .rentAmount(
                                            allocation
                                                    .getMonthlyRent())
                                    .periodStart(
                                            cycleStart)
                                    .periodEnd(
                                            cycleEnd)
                                    .dueDate(
                                            dueDate)
                                    .status(status)
                                    .paymentMode(null)
                                    .paymentNote(null)
                                    .build();

                    rentRecordRepository.save(rent);
                }

                cycleStart =
                        cycleStart.plusMonths(1);
            }
        }
    }

    // =========================================================
    // OWNER
    // =========================================================

    private Owner getOwner(String email) {

        return ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Owner not found"));
    }

    // =========================================================
    // OWNER ISOLATION
    // =========================================================

    private void validateTenantOwner(
            Tenant tenant,
            Owner owner) {

        if (tenant == null
                || tenant.getOwner() == null
                || !tenant.getOwner()
                .getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "Rent record not found");
        }
    }

    // =========================================================
    // ALLOCATION OWNERSHIP
    // =========================================================

    private void validateAllocationOwnership(
            TenantAllocation allocation,
            Owner owner) {

        if (allocation == null
                || allocation.getTenant() == null
                || allocation.getTenant().getOwner() == null
                || allocation.getPg() == null
                || allocation.getPg().getOwner() == null
                || allocation.getRoom() == null
                || allocation.getRoom().getPg() == null
                || allocation.getBed() == null
                || allocation.getBed().getRoom() == null) {

            throw new RuntimeException(
                    "Invalid tenant allocation");
        }

        Long tenantOwnerId =
                allocation.getTenant()
                        .getOwner()
                        .getId();

        Long pgOwnerId =
                allocation.getPg()
                        .getOwner()
                        .getId();

        boolean roomBelongsToAllocationPg = Objects.equals(
                allocation.getRoom().getPg().getId(), allocation.getPg().getId());
        boolean bedBelongsToAllocationRoom = Objects.equals(
                allocation.getBed().getRoom().getId(), allocation.getRoom().getId());

        if (!Objects.equals(tenantOwnerId, owner.getId())
                || !Objects.equals(pgOwnerId, owner.getId())
                || !roomBelongsToAllocationPg
                || !bedBelongsToAllocationRoom) {

            throw new RuntimeException(
                    "Invalid tenant allocation");
        }
    }

    // =========================================================
    // RESPONSE MAPPER
    // =========================================================

    private RentResponse mapToResponse(
            RentRecord rent) {

        return RentResponse.builder()
                .id(rent.getId())
                .tenantId(
                        rent.getTenant().getId())
                .tenantName(
                        rent.getTenant().getName())
                .pgId(
                        rent.getPg().getId())
                .pgName(
                        rent.getPg().getName())
                .rentAmount(
                        rent.getRentAmount())
                .periodStart(
                        rent.getPeriodStart())
                .periodEnd(
                        rent.getPeriodEnd())
                .dueDate(
                        rent.getDueDate())
                .paidDate(
                        rent.getPaidDate())
                .status(
                        rent.getStatus())
                .paymentMode(
                        rent.getPaymentMode())
                .paymentNote(
                        rent.getPaymentNote())
                .build();
    }
}