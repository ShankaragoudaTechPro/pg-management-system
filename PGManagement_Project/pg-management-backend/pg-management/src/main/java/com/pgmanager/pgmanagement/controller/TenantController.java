package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.TenantRequest;
import com.pgmanager.pgmanagement.dto.TenantResponse;
import com.pgmanager.pgmanagement.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    // =========================================================
    // CREATE TENANT
    // =========================================================

    @PostMapping
    public ResponseEntity<TenantResponse> createTenant(
            Authentication authentication,
            @Valid @RequestBody TenantRequest request) {

        String ownerEmail = authentication.getName();

        TenantResponse response =
                tenantService.createTenant(
                        ownerEmail,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET TENANTS / SEARCH + PAGINATION
    // =========================================================
    @GetMapping
    public ResponseEntity<?> getMyTenants(
            Authentication authentication,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) Long pgId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        String ownerEmail = authentication.getName();
        boolean paginationRequested = page != null || size != null
                || search != null || active != null || pgId != null;

        // The feature UI expects a plain array when no filters are supplied.
        if (!paginationRequested) {
            return ResponseEntity.ok(tenantService.getMyTenants(ownerEmail));
        }

        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 10 : size;
        if (pageNumber < 0) {
            throw new IllegalArgumentException("Page number cannot be negative");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize,
                Sort.by(Sort.Direction.DESC, "createdAt"));
        if (search != null && !search.trim().isEmpty()) {
            return ResponseEntity.ok(tenantService.searchTenants(ownerEmail, search, pageable));
        }
        if (active != null || pgId != null) {
            return ResponseEntity.ok(tenantService.filterTenants(ownerEmail, active, pgId, pageable));
        }
        return ResponseEntity.ok(tenantService.getMyTenants(ownerEmail, pageable));
    }
    // =========================================================
    // GET ONE TENANT
    // =========================================================

    @GetMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> getTenantById(
            Authentication authentication,
            @PathVariable Long tenantId) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok(
                tenantService.getTenantById(
                        ownerEmail,
                        tenantId
                )
        );
    }

    // =========================================================
    // UPDATE TENANT PROFILE / RENT (allocation stays the same)
    // =========================================================

    @PutMapping("/{tenantId}")
    public ResponseEntity<TenantResponse> updateTenant(
            Authentication authentication,
            @PathVariable Long tenantId,
            @Valid @RequestBody TenantRequest request) {

        return ResponseEntity.ok(tenantService.updateTenant(
                authentication.getName(), tenantId, request));
    }

    // Safe removal: preserve tenant/rent history and release the bed.
    @DeleteMapping("/{tenantId}")
    public ResponseEntity<Void> removeTenant(
            Authentication authentication,
            @PathVariable Long tenantId) {

        tenantService.checkoutTenant(authentication.getName(), tenantId);
        return ResponseEntity.noContent().build();
    }

    // =========================================================
    // CHECKOUT TENANT
    // =========================================================

    @PatchMapping("/{tenantId}/checkout")
    public ResponseEntity<String> checkoutTenant(
            Authentication authentication,
            @PathVariable Long tenantId) {

        String ownerEmail =
                authentication.getName();

        tenantService.checkoutTenant(
                ownerEmail,
                tenantId
        );

        return ResponseEntity.ok(
                "Tenant checked out successfully"
        );
    }
}