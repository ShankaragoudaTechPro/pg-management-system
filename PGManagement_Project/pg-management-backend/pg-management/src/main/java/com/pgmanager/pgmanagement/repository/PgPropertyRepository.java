package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.PgProperty;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PgPropertyRepository extends JpaRepository<PgProperty, Long> {

    List<PgProperty> findAllByOwner(Owner owner);

    Optional<PgProperty> findByIdAndOwner(Long id, Owner owner);

    boolean existsByNameAndOwner(String name, Owner owner);
}