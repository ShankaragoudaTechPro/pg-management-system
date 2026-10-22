
package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.SecurityAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SecurityAuditLogRepository
        extends JpaRepository<SecurityAuditLog, Long> {

    List<SecurityAuditLog> findAllByTargetOwnerIdOrderByPerformedAtDesc(
            Long targetOwnerId
    );
}
