package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Facility;
import com.pgmanager.pgmanagement.entity.Owner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacilityRepository
        extends JpaRepository<Facility, Long> {

    /*
     * Global facilities + owner's custom facilities
     */
    @Query("""
            SELECT f
            FROM Facility f
            WHERE f.active = true
              AND (f.owner IS NULL OR f.owner = :owner)
            ORDER BY f.name
            """)
    List<Facility> findAvailableFacilities(
            @Param("owner") Owner owner);

    /*
     * Find a facility that is either:
     * 1. Global
     * 2. Owned by the current owner
     */
    @Query("""
            SELECT f
            FROM Facility f
            WHERE f.id = :facilityId
              AND (f.owner IS NULL OR f.owner = :owner)
            """)
    Optional<Facility> findAccessibleFacility(
            @Param("facilityId") Long facilityId,
            @Param("owner") Owner owner);

    /*
     * Check custom facility name for the same owner.
     */
    boolean existsByOwnerAndNameIgnoreCase(
            Owner owner,
            String name);

    /*
     * Check global facility name.
     */
    boolean existsByOwnerIsNullAndNameIgnoreCase(
            String name);

    /*
     * Find owner's custom facilities.
     */
    List<Facility> findAllByOwner(Owner owner);
}