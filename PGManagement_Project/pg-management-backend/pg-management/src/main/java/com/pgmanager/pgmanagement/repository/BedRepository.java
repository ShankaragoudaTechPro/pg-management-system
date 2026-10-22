package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.Bed;
import com.pgmanager.pgmanagement.entity.BedStatus;
import com.pgmanager.pgmanagement.entity.Room;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.repository.projection.DashboardBedStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface BedRepository
        extends JpaRepository<Bed, Long> {

    List<Bed> findAllByRoomOrderByBedNumberAsc(
            Room room
    );

    /**
     * Fetch all beds belonging to a PG in one query.
     * This avoids one bed query per room when building the dashboard.
     */
    List<Bed> findAllByRoom_Pg(PgProperty pg);

    @Query("""
        select b.room.pg.id as pgId, b.status as status, count(b.id) as bedCount
        from Bed b
        where b.room.pg.id in :pgIds and b.room.active = true
        group by b.room.pg.id, b.status
        """)
    List<DashboardBedStats> findDashboardBedCounts(
            @Param("pgIds") Collection<Long> pgIds);

    Optional<Bed> findByIdAndRoom(
            Long id,
            Room room
    );

    boolean existsByRoomAndBedNumber(
            Room room,
            Integer bedNumber
    );

    long countByRoomAndStatus(
            Room room,
            BedStatus status
    );
}