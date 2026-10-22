package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.RentRequest;
import com.pgmanager.pgmanagement.dto.MarkRentPaidRequest;
import com.pgmanager.pgmanagement.dto.RentResponse;
import com.pgmanager.pgmanagement.service.RentService;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/rents")
@RequiredArgsConstructor
public class RentController {

    private final RentService rentService;

    // =========================================================
    // CREATE RENT
    // =========================================================

    @PostMapping("/tenant/{tenantId}")
    public ResponseEntity<RentResponse> createRent(
            Authentication authentication,
            @PathVariable Long tenantId,
            @Valid @RequestBody RentRequest request) {

        String ownerEmail =
                authentication.getName();

        RentResponse response =
                rentService.createRent(
                        ownerEmail,
                        tenantId,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // =========================================================
    // GET RENTS
    //
    // Examples:
    //
    // /api/v1/rents
    // /api/v1/rents?page=0&size=10
    // /api/v1/rents?search=Rahul&page=0&size=10
    // /api/v1/rents?status=PENDING&page=0&size=10
    // /api/v1/rents?pgId=2&page=0&size=10
    // =========================================================

    @GetMapping
    public ResponseEntity<Page<RentResponse>> getMyRents(
            Authentication authentication,

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            String status,

            @RequestParam(required = false)
            Long pgId) {

        validatePagination(page, size);

        String ownerEmail =
                authentication.getName();

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "dueDate"
                        )
                );

        Page<RentResponse> response;

        /*
         * Search has priority.
         * Otherwise status / PG filters are applied.
         */
        if (search != null &&
                !search.trim().isEmpty()) {

            response =
                    rentService.searchRents(
                            ownerEmail,
                            search,
                            pageable
                    );

        } else if (status != null ||
                pgId != null) {

            response =
                    rentService.filterRents(
                            ownerEmail,
                            status,
                            pgId,
                            pageable
                    );

        } else {

            response =
                    rentService.getMyRents(
                            ownerEmail,
                            pageable
                    );
        }

        return ResponseEntity.ok(response);
    }

    // =========================================================
    // TENANT RENT HISTORY
    // =========================================================

    @GetMapping("/tenant/{tenantId}")
    public ResponseEntity<List<RentResponse>> getTenantRents(
            Authentication authentication,
            @PathVariable Long tenantId) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok(
                rentService.getTenantRents(
                        ownerEmail,
                        tenantId
                )
        );
    }

    // =========================================================
    // GET RENT BY ID
    // =========================================================

    @GetMapping("/{rentId}")
    public ResponseEntity<RentResponse> getRentById(
            Authentication authentication,
            @PathVariable Long rentId) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok(
                rentService.getRentById(
                        ownerEmail,
                        rentId
                )
        );
    }

    // =========================================================
    // MARK PAID
    // =========================================================

    @PatchMapping("/{rentId}/paid")
    public ResponseEntity<RentResponse> markRentAsPaid(
            Authentication authentication,
            @PathVariable Long rentId,
            @Valid @RequestBody MarkRentPaidRequest request) {

        String ownerEmail =
                authentication.getName();

        return ResponseEntity.ok(
                rentService.markRentAsPaid(
                        ownerEmail,
                        rentId,
                        request
                )
        );
    }

    // =========================================================
    // PAGINATION VALIDATION
    // =========================================================

    private void validatePagination(
            int page,
            int size) {

        if (page < 0) {
            throw new RuntimeException(
                    "Page must be greater than or equal to 0");
        }

        if (size < 1 || size > 100) {
            throw new RuntimeException(
                    "Size must be between 1 and 100");
        }
    }
}