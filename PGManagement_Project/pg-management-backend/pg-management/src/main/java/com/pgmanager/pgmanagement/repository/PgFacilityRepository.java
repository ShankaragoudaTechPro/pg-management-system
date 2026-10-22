package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Facility;
import com.pgmanager.pgmanagement.entity.PgFacility;
import com.pgmanager.pgmanagement.entity.PgProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PgFacilityRepository
        extends JpaRepository<PgFacility, Long> {

    List<PgFacility> findAllByPg(PgProperty pg);

    Optional<PgFacility> findByPgAndFacility(
            PgProperty pg,
            Facility facility
    );

    boolean existsByPgAndFacility(
            PgProperty pg,
            Facility facility
    );

    void deleteByPg(PgProperty pg);
}