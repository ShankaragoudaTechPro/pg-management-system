package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.pg.PgPropertyRequest;
import com.pgmanager.pgmanagement.dto.pg.PgPropertyResponse;
import com.pgmanager.pgmanagement.dto.pg.PgPropertyUpdateRequest;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.entity.PgStatus;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.repository.PgPropertyRepository;
import com.pgmanager.pgmanagement.service.PgPropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PgPropertyServiceImpl implements PgPropertyService {

    private final PgPropertyRepository pgPropertyRepository;
    private final OwnerRepository ownerRepository;

    @Override
    public PgPropertyResponse createPg(
            String ownerEmail,
            PgPropertyRequest request) {

        Owner owner = getOwner(ownerEmail);

        if (pgPropertyRepository.existsByNameAndOwner(
                request.getName(),
                owner)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A PG with this name already exists for your account.");
        }

        PgProperty pg = PgProperty.builder()
                .owner(owner)
                .name(request.getName())
                .type(request.getType())
                .description(request.getDescription())
                .build();

        PgProperty savedPg =
                pgPropertyRepository.save(pg);

        return mapToResponse(savedPg);
    }

    @Override
    public List<PgPropertyResponse> getMyPgs(
            String ownerEmail) {

        Owner owner = getOwner(ownerEmail);

        return pgPropertyRepository
                .findAllByOwner(owner)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public PgPropertyResponse getMyPgById(
            String ownerEmail,
            Long pgId) {

        Owner owner = getOwner(ownerEmail);

        PgProperty pg = pgPropertyRepository
                .findByIdAndOwner(pgId, owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "PG not found"));

        return mapToResponse(pg);
    }

    @Override
    public PgPropertyResponse updatePg(
            String ownerEmail,
            Long pgId,
            PgPropertyUpdateRequest request) {

        Owner owner = getOwner(ownerEmail);

        PgProperty pg = pgPropertyRepository
                .findByIdAndOwner(pgId, owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "PG not found"));

        if (!pg.getName().equals(request.getName())
                && pgPropertyRepository.existsByNameAndOwner(
                request.getName(),
                owner)) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A PG with this name already exists for your account.");
        }

        pg.setName(request.getName());
        pg.setType(request.getType());
        pg.setDescription(request.getDescription());

        PgProperty updatedPg =
                pgPropertyRepository.save(pg);

        return mapToResponse(updatedPg);
    }

    @Override
    public PgPropertyResponse updatePgStatus(
            String ownerEmail,
            Long pgId) {

        Owner owner = getOwner(ownerEmail);

        PgProperty pg = pgPropertyRepository
                .findByIdAndOwner(pgId, owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "PG not found"));

        if (pg.getStatus() == PgStatus.ACTIVE) {
            pg.setStatus(PgStatus.INACTIVE);
        } else {
            pg.setStatus(PgStatus.ACTIVE);
        }

        PgProperty updatedPg =
                pgPropertyRepository.save(pg);

        return mapToResponse(updatedPg);
    }

    private Owner getOwner(String email) {

        return ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Owner not found"));
    }

    private PgPropertyResponse mapToResponse(
            PgProperty pg) {

        return PgPropertyResponse.builder()
                .id(pg.getId())
                .name(pg.getName())
                .type(pg.getType())
                .description(pg.getDescription())
                .status(pg.getStatus())
                .createdAt(pg.getCreatedAt())
                .updatedAt(pg.getUpdatedAt())
                .build();
    }
}