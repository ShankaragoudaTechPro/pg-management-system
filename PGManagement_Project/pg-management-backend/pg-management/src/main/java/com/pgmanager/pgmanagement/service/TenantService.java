package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.TenantRequest;
import com.pgmanager.pgmanagement.dto.TenantResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TenantService {

    TenantResponse createTenant(
            String ownerEmail,
            TenantRequest request
    );

    List<TenantResponse> getMyTenants(
            String ownerEmail
    );

    List<TenantResponse> searchTenants(
            String ownerEmail,
            String search
    );

    Page<TenantResponse> getMyTenants(
            String ownerEmail,
            Pageable pageable
    );

    Page<TenantResponse> searchTenants(
            String ownerEmail,
            String search,
            Pageable pageable
    );

    Page<TenantResponse> filterTenants(
            String ownerEmail,
            Boolean active,
            Long pgId,
            Pageable pageable
    );

    TenantResponse getTenantById(
            String ownerEmail,
            Long tenantId
    );

    TenantResponse updateTenant(
            String ownerEmail,
            Long tenantId,
            TenantRequest request
    );

    void checkoutTenant(
            String ownerEmail,
            Long tenantId
    );
}