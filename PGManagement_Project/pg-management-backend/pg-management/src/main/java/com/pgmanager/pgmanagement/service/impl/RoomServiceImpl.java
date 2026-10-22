package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.dto.BedResponse;
import com.pgmanager.pgmanagement.dto.RoomRequest;
import com.pgmanager.pgmanagement.dto.RoomResponse;
import com.pgmanager.pgmanagement.entity.Bed;
import com.pgmanager.pgmanagement.entity.BedStatus;
import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.PgProperty;
import com.pgmanager.pgmanagement.entity.PgStatus;
import com.pgmanager.pgmanagement.entity.Room;
import com.pgmanager.pgmanagement.repository.BedRepository;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.repository.PgPropertyRepository;
import com.pgmanager.pgmanagement.repository.RoomRepository;
import com.pgmanager.pgmanagement.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final BedRepository bedRepository;
    private final PgPropertyRepository pgPropertyRepository;
    private final OwnerRepository ownerRepository;

    // =========================================================
    // CREATE ROOM
    // =========================================================

    @Override
    @Transactional
    public RoomResponse createRoom(
            Long pgId,
            String ownerEmail,
            RoomRequest request) {

        PgProperty pg = getOwnerPg(pgId, ownerEmail);

        // Cannot create a new room inside an inactive PG
        if (pg.getStatus() != PgStatus.ACTIVE) {
            throw new RuntimeException(
                    "Cannot create room in inactive PG");
        }

        if (roomRepository.existsByPgAndRoomNumber(
                pg,
                request.getRoomNumber())) {

            throw new RuntimeException(
                    "Room number already exists in this PG");
        }

        Room room = Room.builder()
                .pg(pg)
                .roomNumber(request.getRoomNumber())
                .sharing(request.getSharing())
                .floor(request.getFloor())
                .active(true)
                .build();

        Room savedRoom = roomRepository.save(room);

        // Create beds according to sharing
        for (int i = 1; i <= request.getSharing(); i++) {

            Bed bed = Bed.builder()
                    .room(savedRoom)
                    .bedNumber(i)
                    .status(BedStatus.AVAILABLE)
                    .build();

            bedRepository.save(bed);
        }

        return mapToResponse(savedRoom);
    }

    // =========================================================
    // GET ROOMS BY PG
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getRoomsByPg(
            Long pgId,
            String ownerEmail) {

        PgProperty pg = getOwnerPg(pgId, ownerEmail);

        List<Room> rooms = roomRepository.findAllByPg(pg);
        Map<Long, List<Bed>> bedsByRoomId = bedRepository.findAllByRoom_Pg(pg)
                .stream()
                .collect(Collectors.groupingBy(bed -> bed.getRoom().getId()));

        return rooms.stream()
                .map(room -> mapToResponse(
                        room,
                        bedsByRoomId.getOrDefault(room.getId(), List.of())))
                .toList();
    }

    // =========================================================
    // GET ROOM BY ID
    // =========================================================

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomById(
            Long pgId,
            Long roomId,
            String ownerEmail) {

        PgProperty pg = getOwnerPg(pgId, ownerEmail);

        Room room = roomRepository
                .findByIdAndPg(roomId, pg)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Room not found"));

        return mapToResponse(room);
    }

    // =========================================================
    // UPDATE ROOM
    // =========================================================

    @Override
    @Transactional
    public RoomResponse updateRoom(
            Long pgId,
            Long roomId,
            String ownerEmail,
            RoomRequest request) {

        PgProperty pg = getOwnerPg(pgId, ownerEmail);

        Room room = roomRepository
                .findByIdAndPg(roomId, pg)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Room not found"));

        if (!room.getRoomNumber()
                .equals(request.getRoomNumber())
                && roomRepository.existsByPgAndRoomNumber(
                pg,
                request.getRoomNumber())) {

            throw new RuntimeException(
                    "Room number already exists in this PG");
        }

        long occupiedBeds =
                bedRepository.countByRoomAndStatus(
                        room,
                        BedStatus.OCCUPIED);

        if (request.getSharing() < occupiedBeds) {

            throw new RuntimeException(
                    "Sharing cannot be less than occupied beds");
        }

        room.setRoomNumber(request.getRoomNumber());
        room.setSharing(request.getSharing());
        room.setFloor(request.getFloor());

        Room updatedRoom =
                roomRepository.save(room);

        List<Bed> beds =
                bedRepository
                        .findAllByRoomOrderByBedNumberAsc(room);

        int requiredBeds = request.getSharing();

        // =====================================================
        // ACTIVATE EXISTING INACTIVE BEDS
        // =====================================================

        for (Bed bed : beds) {

            if (bed.getBedNumber() <= requiredBeds
                    && bed.getStatus() == BedStatus.INACTIVE) {

                bed.setStatus(BedStatus.AVAILABLE);
            }
        }

        // =====================================================
        // CREATE NEW BEDS
        // =====================================================

        for (int i = 1; i <= requiredBeds; i++) {

            int bedNumber = i;

            boolean exists = beds.stream()
                    .anyMatch(bed ->
                            bed.getBedNumber()
                                    .equals(bedNumber));

            if (!exists) {

                Bed bed = Bed.builder()
                        .room(room)
                        .bedNumber(bedNumber)
                        .status(BedStatus.AVAILABLE)
                        .build();

                bedRepository.save(bed);
            }
        }

        // =====================================================
        // DEACTIVATE EXTRA AVAILABLE BEDS
        // =====================================================

        for (Bed bed : beds) {

            if (bed.getBedNumber() > requiredBeds
                    && bed.getStatus() == BedStatus.AVAILABLE) {

                bed.setStatus(BedStatus.INACTIVE);
            }
        }

        bedRepository.saveAll(beds);

        return mapToResponse(updatedRoom);
    }

    // =========================================================
    // DEACTIVATE ROOM
    // =========================================================

    @Override
    @Transactional
    public void deactivateRoom(
            Long pgId,
            Long roomId,
            String ownerEmail) {

        PgProperty pg =
                getOwnerPg(pgId, ownerEmail);

        Room room =
                roomRepository
                        .findByIdAndPg(roomId, pg)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Room not found"));

        long occupiedBeds =
                bedRepository.countByRoomAndStatus(
                        room,
                        BedStatus.OCCUPIED);

        if (occupiedBeds > 0) {

            throw new RuntimeException(
                    "Cannot deactivate room with occupied beds");
        }

        room.setActive(false);

        roomRepository.save(room);

        List<Bed> beds =
                bedRepository
                        .findAllByRoomOrderByBedNumberAsc(room);

        for (Bed bed : beds) {
            bed.setStatus(BedStatus.INACTIVE);
        }

        bedRepository.saveAll(beds);
    }

    // =========================================================
    // GET OWNER'S PG
    // =========================================================

    private PgProperty getOwnerPg(
            Long pgId,
            String ownerEmail) {

        Owner owner =
                ownerRepository
                        .findByEmail(ownerEmail)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Owner not found"));

        return pgPropertyRepository
                .findByIdAndOwner(pgId, owner)
                .orElseThrow(() ->
                        new RuntimeException(
                                "PG not found"));
    }

    // =========================================================
    // MAP ROOM RESPONSE
    // =========================================================

    private RoomResponse mapToResponse(Room room) {
        List<Bed> beds = bedRepository.findAllByRoomOrderByBedNumberAsc(room);
        return mapToResponse(room, beds);
    }

    private RoomResponse mapToResponse(Room room, List<Bed> bedEntities) {
        List<BedResponse> beds = bedEntities.stream()
                .sorted(java.util.Comparator.comparing(Bed::getBedNumber))
                .map(this::mapBedToResponse)
                .toList();

        return RoomResponse.builder()
                .id(room.getId())
                .pgId(room.getPg().getId())
                .roomNumber(room.getRoomNumber())
                .sharing(room.getSharing())
                .floor(room.getFloor())
                .active(room.isActive())
                .beds(beds)
                .createdAt(room.getCreatedAt())
                .updatedAt(room.getUpdatedAt())
                .build();
    }

    // =========================================================
    // MAP BED RESPONSE
    // =========================================================

    private BedResponse mapBedToResponse(
            Bed bed) {

        return BedResponse.builder()
                .id(bed.getId())
                .roomId(bed.getRoom().getId())
                .bedNumber(bed.getBedNumber())
                .status(bed.getStatus())
                .build();
    }
}