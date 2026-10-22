package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.RoomRequest;
import com.pgmanager.pgmanagement.dto.RoomResponse;
import com.pgmanager.pgmanagement.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/pgs/{pgId}/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomResponse> createRoom(
            Authentication authentication,
            @PathVariable Long pgId,
            @Valid @RequestBody RoomRequest request) {

        String ownerEmail = authentication.getName();

        RoomResponse response =
                roomService.createRoom(
                        pgId,
                        ownerEmail,
                        request
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<List<RoomResponse>> getRooms(
            Authentication authentication,
            @PathVariable Long pgId) {

        String ownerEmail = authentication.getName();

        return ResponseEntity.ok(
                roomService.getRoomsByPg(
                        pgId,
                        ownerEmail
                )
        );
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> getRoom(
            Authentication authentication,
            @PathVariable Long pgId,
            @PathVariable Long roomId) {

        String ownerEmail = authentication.getName();

        return ResponseEntity.ok(
                roomService.getRoomById(
                        pgId,
                        roomId,
                        ownerEmail
                )
        );
    }

    @PutMapping("/{roomId}")
    public ResponseEntity<RoomResponse> updateRoom(
            Authentication authentication,
            @PathVariable Long pgId,
            @PathVariable Long roomId,
            @Valid @RequestBody RoomRequest request) {

        String ownerEmail = authentication.getName();

        return ResponseEntity.ok(
                roomService.updateRoom(
                        pgId,
                        roomId,
                        ownerEmail,
                        request
                )
        );
    }

    @PatchMapping("/{roomId}/deactivate")
    public ResponseEntity<String> deactivateRoom(
            Authentication authentication,
            @PathVariable Long pgId,
            @PathVariable Long roomId) {

        String ownerEmail = authentication.getName();

        roomService.deactivateRoom(
                pgId,
                roomId,
                ownerEmail
        );

        return ResponseEntity.ok(
                "Room deactivated successfully"
        );
    }
}