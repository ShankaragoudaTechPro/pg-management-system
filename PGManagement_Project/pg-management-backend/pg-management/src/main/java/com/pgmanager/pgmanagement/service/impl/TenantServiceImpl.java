package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.TenantRequest;
import com.pgmanager.pgmanagement.dto.TenantResponse;
import com.pgmanager.pgmanagement.entity.*;
import com.pgmanager.pgmanagement.repository.*;
import com.pgmanager.pgmanagement.service.TenantService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantServiceImpl implements TenantService {

    private final TenantRepository tenantRepository;
    private final TenantAllocationRepository allocationRepository;
    private final OwnerRepository ownerRepository;
    private final PgPropertyRepository pgPropertyRepository;
    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;

    // =========================================================
    // CREATE TENANT
    // =========================================================

    @Override
    @Transactional
    public TenantResponse createTenant(
            String ownerEmail,
            TenantRequest request) {

        Owner owner = getOwner(ownerEmail);

        // Duplicate mobile check for the same owner
        if (tenantRepository.existsByMobileAndOwner(
                request.getMobile(), owner)) {

            throw new RuntimeException(
                    "Tenant with this mobile number already exists");
        }

        // Get PG belonging to the logged-in owner
        PgProperty pg = pgPropertyRepository
                .findByIdAndOwner(request.getPgId(), owner)
                .orElseThrow(() ->
                        new RuntimeException("PG not found"));

        if (pg.getStatus() != PgStatus.ACTIVE) {
            throw new RuntimeException(
                    "Cannot add tenant to inactive PG");
        }

        // Get room belonging to this PG
        Room room = roomRepository
                .findByIdAndPg(request.getRoomId(), pg)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Room not found in this PG"));

        if (!room.isActive()) {
            throw new RuntimeException("Room is inactive");
        }

        // Get bed belonging to this room
        Bed bed = bedRepository
                .findByIdAndRoom(request.getBedId(), room)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Bed not found in this room"));

        if (bed.getStatus() != BedStatus.AVAILABLE) {
            throw new RuntimeException(
                    "Bed is not available");
        }

        if (allocationRepository.existsByBedAndStatus(
                bed, AllocationStatus.ACTIVE)) {
            throw new RuntimeException(
                    "Bed already has an active tenant allocation");
        }

        // Create tenant and save Aadhaar directly
        Tenant tenant = Tenant.builder()
                .owner(owner)
                .name(request.getName())
                .mobile(request.getMobile())
                .email(request.getEmail())
                .gender(request.getGender())
                .joiningDate(request.getJoiningDate())
                .emergencyContact(request.getEmergencyContact())
                .emergencyContactName(
                        request.getEmergencyContactName())
                .aadhaarNumber(request.getAadhaarNumber())
                .active(true)
                .build();

        Tenant savedTenant = tenantRepository.save(tenant);

        // Create active allocation
        TenantAllocation allocation =
                TenantAllocation.builder()
                        .tenant(savedTenant)
                        .pg(pg)
                        .room(room)
                        .bed(bed)
                        .startDate(request.getJoiningDate())
                        .status(AllocationStatus.ACTIVE)
                        .monthlyRent(request.getMonthlyRent())
                        .securityDeposit(
                                request.getSecurityDeposit())
                        .build();

        allocationRepository.save(allocation);

        // Mark bed occupied
        bed.setStatus(BedStatus.OCCUPIED);
        bedRepository.save(bed);

        return mapToResponse(savedTenant, allocation);
    }

    // =========================================================
    // GET ALL TENANTS - NON-PAGINATED
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> getMyTenants(
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        return tenantRepository.findAllByOwner(owner)
                .stream()
                .map(this::mapTenant)
                .toList();
    }

    // =========================================================
    // GET ALL TENANTS - PAGINATION
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<TenantResponse> getMyTenants(
            String ownerEmail,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        return tenantRepository
                .findAllByOwner(owner, pageable)
                .map(this::mapTenant);
    }

    // =========================================================
    // SEARCH TENANTS - NON-PAGINATED
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<TenantResponse> searchTenants(
            String ownerEmail,
            String search) {

        Owner owner = getOwner(ownerEmail);

        String keyword = search == null ? "" : search.trim();

        if (keyword.isEmpty()) {
            return getMyTenants(ownerEmail);
        }

        return tenantRepository
                .searchTenants(owner, keyword)
                .stream()
                .map(this::mapTenant)
                .toList();
    }

    // =========================================================
    // SEARCH TENANTS - PAGINATION
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<TenantResponse> searchTenants(
            String ownerEmail,
            String search,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        String keyword = search == null ? "" : search.trim();

        if (keyword.isEmpty()) {
            return tenantRepository
                    .findAllByOwner(owner, pageable)
                    .map(this::mapTenant);
        }

        return tenantRepository
                .searchTenants(owner, keyword, pageable)
                .map(this::mapTenant);
    }

    // =========================================================
    // FILTER TENANTS
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public Page<TenantResponse> filterTenants(
            String ownerEmail,
            Boolean active,
            Long pgId,
            Pageable pageable) {

        Owner owner = getOwner(ownerEmail);

        return tenantRepository
                .filterTenants(owner, active, pgId, pageable)
                .map(this::mapTenant);
    }

    // =========================================================
    // GET TENANT BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public TenantResponse getTenantById(
            String ownerEmail,
            Long tenantId) {

        Owner owner = getOwner(ownerEmail);

        Tenant tenant = tenantRepository
                .findByIdAndOwner(tenantId, owner)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found"));

        return mapTenant(tenant);
    }

    // =========================================================
    // UPDATE TENANT (keeps the existing allocation)
    // =========================================================

    @Override
    @Transactional
    public TenantResponse updateTenant(
            String ownerEmail,
            Long tenantId,
            TenantRequest request) {

        Owner owner = getOwner(ownerEmail);
        Tenant tenant = tenantRepository.findByIdAndOwner(tenantId, owner)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        if (!tenant.getMobile().equals(request.getMobile())
                && tenantRepository.existsByMobileAndOwner(request.getMobile(), owner)) {
            throw new RuntimeException("Tenant with this mobile number already exists");
        }

        if (!tenant.isActive()) {
            throw new RuntimeException("Checked-out tenants cannot be edited. Add a new tenant allocation instead.");
        }

        TenantAllocation allocation = allocationRepository
                .findByTenantAndStatus(tenant, AllocationStatus.ACTIVE)
                .orElseThrow(() -> new RuntimeException("Active allocation not found"));

        if (!allocation.getPg().getId().equals(request.getPgId())
                || !allocation.getRoom().getId().equals(request.getRoomId())
                || !allocation.getBed().getId().equals(request.getBedId())) {
            throw new RuntimeException("Changing a tenant's bed is not supported in Edit. Check out the tenant and create a new allocation.");
        }

        tenant.setName(request.getName().trim());
        tenant.setMobile(request.getMobile().trim());
        tenant.setEmail(request.getEmail());
        tenant.setGender(request.getGender());
        tenant.setJoiningDate(request.getJoiningDate());
        tenant.setEmergencyContact(request.getEmergencyContact());
        tenant.setEmergencyContactName(request.getEmergencyContactName());
        if (request.getAadhaarNumber() != null && !request.getAadhaarNumber().isBlank()) {
            tenant.setAadhaarNumber(request.getAadhaarNumber().trim());
        }

        allocation.setMonthlyRent(request.getMonthlyRent());
        allocation.setSecurityDeposit(request.getSecurityDeposit());
        tenantRepository.save(tenant);
        allocationRepository.save(allocation);

        return mapToResponse(tenant, allocation);
    }

    // =========================================================
    // CHECKOUT TENANT
    // =========================================================

    @Override
    @Transactional
    public void checkoutTenant(
            String ownerEmail,
            Long tenantId) {

        Owner owner = getOwner(ownerEmail);

        Tenant tenant = tenantRepository
                .findByIdAndOwner(tenantId, owner)
                .orElseThrow(() ->
                        new RuntimeException("Tenant not found"));

        TenantAllocation allocation =
                allocationRepository
                        .findByTenantAndStatus(
                                tenant,
                                AllocationStatus.ACTIVE)
                        .orElse(null);

        if (allocation != null) {
            allocation.setEndDate(LocalDate.now());
            allocation.setStatus(AllocationStatus.CHECKED_OUT);
            allocationRepository.save(allocation);

            Bed bed = allocation.getBed();
            bed.setStatus(BedStatus.AVAILABLE);
            bedRepository.save(bed);
        }

        // Idempotent soft removal preserves rent and allocation history.
        tenant.setActive(false);
        tenantRepository.save(tenant);
    }

    // =========================================================
    // GET OWNER
    // =========================================================

    private Owner getOwner(String email) {

        return ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Owner not found"));
    }

    // =========================================================
    // MAP TENANT
    // =========================================================

    private TenantResponse mapTenant(Tenant tenant) {

        TenantAllocation allocation =
                allocationRepository
                        .findByTenantAndStatus(
                                tenant,
                                AllocationStatus.ACTIVE)
                        .orElse(null);

        if (allocation == null) {
            return TenantResponse.builder()
                    .id(tenant.getId())
                    .name(tenant.getName())
                    .fullName(tenant.getName())
                    .mobile(tenant.getMobile())
                    .mobileNumber(tenant.getMobile())
                    .email(tenant.getEmail())
                    .gender(tenant.getGender())
                    .joiningDate(tenant.getJoiningDate())
                    .emergencyContact(
                            tenant.getEmergencyContact())
                    .emergencyContactName(
                            tenant.getEmergencyContactName())
                    .active(tenant.isActive())
                    .status(tenant.isActive() ? "ACTIVE" : "INACTIVE")
                    .createdAt(tenant.getCreatedAt())
                    .updatedAt(tenant.getUpdatedAt())
                    .build();
        }

        return mapToResponse(tenant, allocation);
    }

    // =========================================================
    // MAP TENANT WITH ACTIVE ALLOCATION
    // =========================================================

    private TenantResponse mapToResponse(
            Tenant tenant,
            TenantAllocation allocation) {

        return TenantResponse.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .fullName(tenant.getName())
                .mobile(tenant.getMobile())
                .mobileNumber(tenant.getMobile())
                .email(tenant.getEmail())
                .gender(tenant.getGender())
                .joiningDate(tenant.getJoiningDate())
                .emergencyContact(
                        tenant.getEmergencyContact())
                .emergencyContactName(
                        tenant.getEmergencyContactName())
                .pgId(allocation.getPg().getId())
                .pgName(allocation.getPg().getName())
                .roomId(allocation.getRoom().getId())
                .roomNumber(
                        allocation.getRoom().getRoomNumber())
                .bedId(allocation.getBed().getId())
                .bedNumber(
                        allocation.getBed().getBedNumber())
                .monthlyRent(allocation.getMonthlyRent())
                .securityDeposit(
                        allocation.getSecurityDeposit())
                .active(tenant.isActive())
                .status(tenant.isActive() ? "ACTIVE" : "INACTIVE")
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}