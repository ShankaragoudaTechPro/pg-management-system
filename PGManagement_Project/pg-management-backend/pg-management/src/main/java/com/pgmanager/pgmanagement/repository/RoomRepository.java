package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.entity.Room;
import com.pgmanager.pgmanagement.repository.projection.DashboardRoomStats;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByPg(PgProperty pg);

    @Query("""
        select r.pg.id as pgId, count(r.id) as totalRooms
        from Room r
        where r.pg.id in :pgIds and r.active = true
        group by r.pg.id
        """)
    List<DashboardRoomStats> findDashboardRoomCounts(
            @Param("pgIds") Collection<Long> pgIds);

    Optional<Room> findByIdAndPg(
            Long id,
            PgProperty pg);

    boolean existsByPgAndRoomNumber(
            PgProperty pg,
            String roomNumber);
}