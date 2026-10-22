package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.RentRequest;
import com.pgmanager.pgmanagement.dto.MarkRentPaidRequest;
import com.pgmanager.pgmanagement.dto.RentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RentService {

    RentResponse createRent(
            String ownerEmail,
            Long tenantId,
            RentRequest request
    );

    List<RentResponse> getMyRents(
            String ownerEmail
    );

    Page<RentResponse> getMyRents(
            String ownerEmail,
            Pageable pageable
    );

    Page<RentResponse> searchRents(
            String ownerEmail,
            String search,
            Pageable pageable
    );

    Page<RentResponse> filterRents(
            String ownerEmail,
            String status,
            Long pgId,
            Pageable pageable
    );

    List<RentResponse> getTenantRents(
            String ownerEmail,
            Long tenantId
    );

    RentResponse getRentById(
            String ownerEmail,
            Long rentId
    );

    RentResponse markRentAsPaid(
            String ownerEmail,
            Long rentId,
            MarkRentPaidRequest request
    );

    void updateOverdueRents();

    void generateMonthlyRents();
}