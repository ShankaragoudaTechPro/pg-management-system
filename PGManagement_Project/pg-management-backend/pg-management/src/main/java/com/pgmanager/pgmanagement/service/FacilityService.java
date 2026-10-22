package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.FacilityRequest;
import com.pgmanager.pgmanagement.dto.FacilityResponse;

import java.util.List;

public interface FacilityService {

    FacilityResponse createFacility(
            String ownerEmail,
            FacilityRequest request);

    List<FacilityResponse> getAllFacilities(
            String ownerEmail);

    FacilityResponse getFacilityById(
            String ownerEmail,
            Long id);

    FacilityResponse updateFacility(
            String ownerEmail,
            Long id,
            FacilityRequest request);

    void deactivateFacility(
            String ownerEmail,
            Long id);

    List<FacilityResponse> getFacilitiesByPg(
            Long pgId,
            String ownerEmail);

    void addFacilityToPg(
            Long pgId,
            Long facilityId,
            String ownerEmail);

    void removeFacilityFromPg(
            Long pgId,
            Long facilityId,
            String ownerEmail);
}