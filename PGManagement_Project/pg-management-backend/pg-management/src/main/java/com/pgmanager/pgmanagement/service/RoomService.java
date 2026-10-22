package com.pgmanager.pgmanagement.service;

import com.pgmanager.pgmanagement.dto.RoomRequest;
import com.pgmanager.pgmanagement.dto.RoomResponse;

import java.util.List;

public interface RoomService {

    RoomResponse createRoom(
            Long pgId,
            String ownerEmail,
            RoomRequest request
    );

    List<RoomResponse> getRoomsByPg(
            Long pgId,
            String ownerEmail
    );

    RoomResponse getRoomById(
            Long pgId,
            Long roomId,
            String ownerEmail
    );

    RoomResponse updateRoom(
            Long pgId,
            Long roomId,
            String ownerEmail,
            RoomRequest request
    );

    void deactivateRoom(
            Long pgId,
            Long roomId,
            String ownerEmail
    );
}