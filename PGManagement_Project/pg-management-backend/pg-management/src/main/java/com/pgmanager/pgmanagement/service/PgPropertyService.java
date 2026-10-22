package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.pg.PgPropertyRequest;
import com.pgmanager.pgmanagement.dto.pg.PgPropertyResponse;
import com.pgmanager.pgmanagement.dto.pg.PgPropertyUpdateRequest;

import java.util.List;

public interface PgPropertyService {

    PgPropertyResponse createPg(
            String ownerEmail,
            PgPropertyRequest request
    );

    List<PgPropertyResponse> getMyPgs(
            String ownerEmail
    );

    PgPropertyResponse getMyPgById(
            String ownerEmail,
            Long pgId
    );

    PgPropertyResponse updatePg(
            String ownerEmail,
            Long pgId,
            PgPropertyUpdateRequest request
    );

    PgPropertyResponse updatePgStatus(
            String ownerEmail,
            Long pgId
    );
}