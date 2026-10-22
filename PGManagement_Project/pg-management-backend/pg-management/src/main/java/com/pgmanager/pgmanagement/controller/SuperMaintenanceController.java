
package com.pgmanager.pgmanagement.controller;

import com.pgmanager.pgmanagement.dto.AccountControlRequest;
import com.pgmanager.pgmanagement.entity.SecurityAuditLog;
import com.pgmanager.pgmanagement.service.impl.SuperMaintenanceAccountControlService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/maintenance")
@RequiredArgsConstructor
public class SuperMaintenanceController {

    private final SuperMaintenanceAccountControlService accountControlService;

    // =====================================================
    // SUSPEND OWNER
    // POST /api/v1/maintenance/owners/{ownerId}/suspend
    // =====================================================

    @PostMapping("/owners/{ownerId}/suspend")
    public ResponseEntity<Map<String, String>> suspendOwner(
            Authentication authentication,
            @PathVariable Long ownerId,
            @Valid @RequestBody AccountControlRequest request) {

        accountControlService.suspendOwner(
                authentication.getName(),
                ownerId,
                request.getReason()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Owner account suspended and sessions revoked"
                )
        );
    }

    // =====================================================
    // DISCONNECT ALL OWNER SESSIONS
    // POST /api/v1/maintenance/owners/{ownerId}/disconnect-sessions
    // =====================================================

    @PostMapping("/owners/{ownerId}/disconnect-sessions")
    public ResponseEntity<Map<String, String>> disconnectAllSessions(
            Authentication authentication,
            @PathVariable Long ownerId,
            @Valid @RequestBody AccountControlRequest request) {

        accountControlService.disconnectAllSessions(
                authentication.getName(),
                ownerId,
                request.getReason()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "All existing owner sessions have been revoked"
                )
        );
    }

    // =====================================================
    // RESTORE OWNER ACCESS
    // POST /api/v1/maintenance/owners/{ownerId}/restore
    // =====================================================

    @PostMapping("/owners/{ownerId}/restore")
    public ResponseEntity<Map<String, String>> restoreOwner(
            Authentication authentication,
            @PathVariable Long ownerId,
            @Valid @RequestBody AccountControlRequest request) {

        accountControlService.restoreOwner(
                authentication.getName(),
                ownerId,
                request.getReason()
        );

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "Owner access restored; previous sessions remain invalid"
                )
        );
    }

    // =====================================================
    // VIEW OWNER SECURITY AUDIT HISTORY
    // GET /api/v1/maintenance/owners/{ownerId}/audit
    // =====================================================

    @GetMapping("/owners/{ownerId}/audit")
    public ResponseEntity<List<SecurityAuditLog>> getAuditHistory(
            Authentication authentication,
            @PathVariable Long ownerId) {

        List<SecurityAuditLog> history =
                accountControlService.getOwnerAuditHistory(
                        authentication.getName(),
                        ownerId
                );

        return ResponseEntity.ok(history);
    }
}
