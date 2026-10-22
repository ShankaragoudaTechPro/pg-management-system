package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.FacilityRequest;
import com.pgmanager.pgmanagement.dto.FacilityResponse;
import com.pgmanager.pgmanagement.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/facilities")
@RequiredArgsConstructor
public class FacilityController {

    private final FacilityService facilityService;

    @PostMapping
    public FacilityResponse createFacility(
            Authentication authentication,
            @RequestBody FacilityRequest request) {

        return facilityService.createFacility(
                authentication.getName(),
                request);
    }

    @GetMapping
    public List<FacilityResponse> getAllFacilities(
            Authentication authentication) {

        return facilityService.getAllFacilities(
                authentication.getName());
    }

    @GetMapping("/{id}")
    public FacilityResponse getFacilityById(
            Authentication authentication,
            @PathVariable Long id) {

        return facilityService.getFacilityById(
                authentication.getName(),
                id);
    }

    @PutMapping("/{id}")
    public FacilityResponse updateFacility(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody FacilityRequest request) {

        return facilityService.updateFacility(
                authentication.getName(),
                id,
                request);
    }

    @PatchMapping("/{id}/deactivate")
    public void deactivateFacility(
            Authentication authentication,
            @PathVariable Long id) {

        facilityService.deactivateFacility(
                authentication.getName(),
                id);
    }

    @GetMapping("/pg/{pgId}")
    public List<FacilityResponse> getFacilitiesByPg(
            Authentication authentication,
            @PathVariable Long pgId) {

        return facilityService.getFacilitiesByPg(
                pgId,
                authentication.getName());
    }

    @PostMapping("/pg/{pgId}/{facilityId}")
    public void addFacilityToPg(
            Authentication authentication,
            @PathVariable Long pgId,
            @PathVariable Long facilityId) {

        facilityService.addFacilityToPg(
                pgId,
                facilityId,
                authentication.getName());
    }

    @DeleteMapping("/pg/{pgId}/{facilityId}")
    public void removeFacilityFromPg(
            Authentication authentication,
            @PathVariable Long pgId,
            @PathVariable Long facilityId) {

        facilityService.removeFacilityFromPg(
                pgId,
                facilityId,
                authentication.getName());
    }
}