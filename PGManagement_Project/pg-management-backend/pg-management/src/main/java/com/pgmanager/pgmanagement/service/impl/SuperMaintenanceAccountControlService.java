
package com.pgmanager.pgmanagement.service.impl;

import com.pgmanager.pgmanagement.entity.Owner;
import com.pgmanager.pgmanagement.entity.OwnerStatus;
import com.pgmanager.pgmanagement.entity.SecurityAuditLog;
import com.pgmanager.pgmanagement.repository.OwnerRepository;
import com.pgmanager.pgmanagement.repository.SecurityAuditLogRepository;
import com.pgmanager.pgmanagement.security.Role;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperMaintenanceAccountControlService {

    private final OwnerRepository ownerRepository;
    private final SecurityAuditLogRepository auditLogRepository;

    // =====================================================
    // SUSPEND OWNER
    // Blocks login and invalidates existing JWT sessions.
    // =====================================================

    @Transactional
    public void suspendOwner(
            String actorEmail,
            Long targetOwnerId,
            String reason) {

        Owner actor = requireSuperMaintenanceActor(actorEmail);
        Owner target = requireTargetOwner(targetOwnerId);
        String validReason = validateReason(reason);

        if (target.getStatus() == OwnerStatus.BLOCKED) {
            throw new IllegalStateException(
                    "Owner account is already suspended");
        }

        target.setStatus(OwnerStatus.BLOCKED);
        target.setSessionVersion(target.getSessionVersion() + 1);

        ownerRepository.save(target);

        recordAudit(
                actor,
                target,
                "OWNER_SUSPENDED",
                validReason
        );
    }

    // =====================================================
    // DISCONNECT ALL OWNER SESSIONS
    // Keeps account status unchanged.
    // =====================================================

    @Transactional
    public void disconnectAllSessions(
            String actorEmail,
            Long targetOwnerId,
            String reason) {

        Owner actor = requireSuperMaintenanceActor(actorEmail);
        Owner target = requireTargetOwner(targetOwnerId);
        String validReason = validateReason(reason);

        target.setSessionVersion(target.getSessionVersion() + 1);

        ownerRepository.save(target);

        recordAudit(
                actor,
                target,
                "OWNER_SESSIONS_REVOKED",
                validReason
        );
    }

    // =====================================================
    // RESTORE OWNER ACCESS
    // Old JWTs remain invalid after restoration.
    // =====================================================

    @Transactional
    public void restoreOwner(
            String actorEmail,
            Long targetOwnerId,
            String reason) {

        Owner actor = requireSuperMaintenanceActor(actorEmail);
        Owner target = requireTargetOwner(targetOwnerId);
        String validReason = validateReason(reason);

        if (target.getStatus() != OwnerStatus.BLOCKED) {
            throw new IllegalStateException(
                    "Only a suspended owner account can be restored");
        }

        target.setStatus(OwnerStatus.ACTIVE);

        // Never reuse tokens issued before suspension.
        target.setSessionVersion(target.getSessionVersion() + 1);

        ownerRepository.save(target);

        recordAudit(
                actor,
                target,
                "OWNER_ACCESS_RESTORED",
                validReason
        );
    }

    // =====================================================
    // GET OWNER SECURITY AUDIT HISTORY
    // =====================================================

    @Transactional(readOnly = true)
    public List<SecurityAuditLog> getOwnerAuditHistory(
            String actorEmail,
            Long targetOwnerId) {

        requireSuperMaintenanceActor(actorEmail);
        requireTargetOwner(targetOwnerId);

        return auditLogRepository
                .findAllByTargetOwnerIdOrderByPerformedAtDesc(
                        targetOwnerId);
    }

    // =====================================================
    // VERIFY AUTHORIZED SUPER MAINTENANCE ACCOUNT
    // =====================================================

    private Owner requireSuperMaintenanceActor(String email) {

        Owner actor = ownerRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Authenticated account not found"));

        if (actor.getRole() != Role.SUPER_MAINTENANCE) {
            throw new SecurityException(
                    "Super Maintenance permission is required");
        }

        if (actor.getStatus() != OwnerStatus.ACTIVE) {
            throw new SecurityException(
                    "Super Maintenance account is not active");
        }

        return actor;
    }

    // =====================================================
    // ALLOW CONTROL OF OWNER ACCOUNTS ONLY
    // =====================================================

    private Owner requireTargetOwner(Long ownerId) {

        if (ownerId == null || ownerId <= 0) {
            throw new IllegalArgumentException(
                    "A valid owner ID is required");
        }

        Owner target = ownerRepository
                .findById(ownerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Owner account not found"));

        if (target.getRole() != Role.OWNER) {
            throw new SecurityException(
                    "Only Owner accounts can be controlled by this service");
        }

        return target;
    }

    // =====================================================
    // VALIDATE REASON
    // =====================================================

    private String validateReason(String reason) {

        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException(
                    "An audit reason is required");
        }

        String cleanedReason = reason.trim();

        if (cleanedReason.length() > 500) {
            throw new IllegalArgumentException(
                    "Reason cannot exceed 500 characters");
        }

        return cleanedReason;
    }

    // =====================================================
    // WRITE AUDIT RECORD
    // =====================================================

    private void recordAudit(
            Owner actor,
            Owner target,
            String action,
            String reason) {

        SecurityAuditLog auditLog = SecurityAuditLog.builder()
                .actorEmail(actor.getEmail())
                .action(action)
                .targetOwnerId(target.getId())
                .targetOwnerEmail(target.getEmail())
                .reason(reason)
                .performedAt(LocalDateTime.now())
                .build();

        auditLogRepository.save(auditLog);
    }
}
