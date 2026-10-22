package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.FacilityRequest;
import com.pgmanager.pgmanagement.dto.FacilityResponse;
import com.pgmanager.pgmanagement.entity.Facility;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.PgFacility;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.repository.FacilityRepository;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.repository.PgFacilityRepository;
import com.pgmanager.pgmanagement.repository.PgPropertyRepository;
import com.pgmanager.pgmanagement.service.FacilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacilityServiceImpl
        implements FacilityService {

    private final FacilityRepository facilityRepository;
    private final PgFacilityRepository pgFacilityRepository;
    private final PgPropertyRepository pgPropertyRepository;
    private final OwnerRepository ownerRepository;

    @Override
    @Transactional
    public FacilityResponse createFacility(
            String ownerEmail,
            FacilityRequest request) {

        Owner owner = getOwner(ownerEmail);

        /*
         * Owner can create only custom facilities.
         */
        if (facilityRepository
                .existsByOwnerAndNameIgnoreCase(
                        owner,
                        request.getName())) {

            throw new RuntimeException(
                    "Facility already exists");
        }

        /*
         * Do not allow custom facility with
         * the same name as a global facility.
         */
        if (facilityRepository
                .existsByOwnerIsNullAndNameIgnoreCase(
                        request.getName())) {

            throw new RuntimeException(
                    "Facility already exists in master list");
        }

        Facility facility = Facility.builder()
                .owner(owner)
                .name(request.getName())
                .active(true)
                .build();

        Facility savedFacility =
                facilityRepository.save(facility);

        return mapToResponse(savedFacility);
    }

    @Override
    public List<FacilityResponse> getAllFacilities(
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        return facilityRepository
                .findAvailableFacilities(owner)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public FacilityResponse getFacilityById(
            String ownerEmail,
            Long id) {

        Owner owner = getOwner(ownerEmail);

        Facility facility =
                facilityRepository
                        .findAccessibleFacility(
                                id,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Facility not found"));

        return mapToResponse(facility);
    }

    @Override
    @Transactional
    public FacilityResponse updateFacility(
            String ownerEmail,
            Long id,
            FacilityRequest request) {

        Owner owner = getOwner(ownerEmail);

        Facility facility =
                facilityRepository
                        .findAccessibleFacility(
                                id,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Facility not found"));

        /*
         * Global facilities cannot be changed
         * by PG owners.
         */
        if (facility.getOwner() == null) {

            throw new RuntimeException(
                    "Global facility cannot be modified");
        }

        if (!facility.getOwner()
                .getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "Facility not found");
        }

        if (!facility.getName()
                .equalsIgnoreCase(request.getName())
                && facilityRepository
                .existsByOwnerAndNameIgnoreCase(
                        owner,
                        request.getName())) {

            throw new RuntimeException(
                    "Facility already exists");
        }

        /*
         * Do not rename a custom facility
         * to a global facility name.
         */
        if (!facility.getName()
                .equalsIgnoreCase(request.getName())
                && facilityRepository
                .existsByOwnerIsNullAndNameIgnoreCase(
                        request.getName())) {

            throw new RuntimeException(
                    "Facility already exists in master list");
        }

        facility.setName(request.getName());

        Facility updatedFacility =
                facilityRepository.save(facility);

        return mapToResponse(updatedFacility);
    }

    @Override
    @Transactional
    public void deactivateFacility(
            String ownerEmail,
            Long id) {

        Owner owner = getOwner(ownerEmail);

        Facility facility =
                facilityRepository
                        .findAccessibleFacility(
                                id,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Facility not found"));

        /*
         * Global facilities cannot be
         * deactivated by owners.
         */
        if (facility.getOwner() == null) {

            throw new RuntimeException(
                    "Global facility cannot be deactivated");
        }

        if (!facility.getOwner()
                .getId()
                .equals(owner.getId())) {

            throw new RuntimeException(
                    "Facility not found");
        }

        facility.setActive(false);

        facilityRepository.save(facility);
    }

    @Override
    public List<FacilityResponse> getFacilitiesByPg(
            Long pgId,
            String ownerEmail) {

        PgProperty pg =
                getOwnerPg(pgId, ownerEmail);

        return pgFacilityRepository
                .findAllByPg(pg)
                .stream()
                .map(PgFacility::getFacility)
                .filter(Facility::isActive)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public void addFacilityToPg(
            Long pgId,
            Long facilityId,
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        PgProperty pg =
                pgPropertyRepository
                        .findByIdAndOwner(
                                pgId,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "PG not found"));

        /*
         * Facility must be either:
         * - Global
         * - Owned by current owner
         */
        Facility facility =
                facilityRepository
                        .findAccessibleFacility(
                                facilityId,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Facility not found"));

        if (!facility.isActive()) {

            throw new RuntimeException(
                    "Facility is not active");
        }

        if (pgFacilityRepository
                .existsByPgAndFacility(
                        pg,
                        facility)) {

            throw new RuntimeException(
                    "Facility already added to this PG");
        }

        PgFacility pgFacility =
                PgFacility.builder()
                        .pg(pg)
                        .facility(facility)
                        .build();

        pgFacilityRepository.save(pgFacility);
    }

    @Override
    @Transactional
    public void removeFacilityFromPg(
            Long pgId,
            Long facilityId,
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        PgProperty pg =
                pgPropertyRepository
                        .findByIdAndOwner(
                                pgId,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "PG not found"));

        Facility facility =
                facilityRepository
                        .findAccessibleFacility(
                                facilityId,
                                owner)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Facility not found"));

        PgFacility pgFacility =
                pgFacilityRepository
                        .findByPgAndFacility(
                                pg,
                                facility)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Facility is not assigned to this PG"));

        pgFacilityRepository.delete(pgFacility);
    }

    private Owner getOwner(String email) {

        return ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Owner not found"));
    }

    private PgProperty getOwnerPg(
            Long pgId,
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        return pgPropertyRepository
                .findByIdAndOwner(
                        pgId,
                        owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "PG not found"));
    }

    private FacilityResponse mapToResponse(
            Facility facility) {

        return FacilityResponse.builder()
                .id(facility.getId())
                .name(facility.getName())
                .active(facility.isActive())
                .build();
    }
}