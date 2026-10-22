package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.pg.PgPropertyRequest;
import com.pgmanager.pgmanagement.dto.pg.PgPropertyResponse;
import com.pgmanager.pgmanagement.dto.pg.PgPropertyUpdateRequest;
import com.pgmanager.pgmanagement.service.PgPropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pgs")
@RequiredArgsConstructor
public class PgPropertyController {

    private final PgPropertyService pgPropertyService;

    // Create PG
    @PostMapping
    public ResponseEntity<PgPropertyResponse> createPg(
            Authentication authentication,
            @Valid @RequestBody PgPropertyRequest request) {

        String ownerEmail = authentication.getName();

        PgPropertyResponse response =
                pgPropertyService.createPg(ownerEmail, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Get all PGs of logged-in owner
    @GetMapping
    public ResponseEntity<List<PgPropertyResponse>> getMyPgs(
            Authentication authentication) {

        String ownerEmail = authentication.getName();

        List<PgPropertyResponse> response =
                pgPropertyService.getMyPgs(ownerEmail);

        return ResponseEntity.ok(response);
    }

    // Get one PG
    @GetMapping("/{id}")
    public ResponseEntity<PgPropertyResponse> getMyPgById(
            Authentication authentication,
            @PathVariable Long id) {

        String ownerEmail = authentication.getName();

        PgPropertyResponse response =
                pgPropertyService.getMyPgById(ownerEmail, id);

        return ResponseEntity.ok(response);
    }

    // Update PG
    @PutMapping("/{id}")
    public ResponseEntity<PgPropertyResponse> updatePg(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PgPropertyUpdateRequest request) {

        String ownerEmail = authentication.getName();

        PgPropertyResponse response =
                pgPropertyService.updatePg(
                        ownerEmail,
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }

    // Activate / Deactivate PG
    @PatchMapping("/{id}/status")
    public ResponseEntity<PgPropertyResponse> updatePgStatus(
            Authentication authentication,
            @PathVariable Long id) {

        String ownerEmail = authentication.getName();

        PgPropertyResponse response =
                pgPropertyService.updatePgStatus(
                        ownerEmail,
                        id
                );

        return ResponseEntity.ok(response);
    }
}