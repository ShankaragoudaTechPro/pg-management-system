
package com.pgmanager.pgmanagement.repository;

import com.pgmanager.pgmanagement.entity.MaintenanceStaff;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MaintenanceStaffRepository
        extends JpaRepository<MaintenanceStaff, Long> {

    Optional<MaintenanceStaff> findByEmail(String email);

    Optional<MaintenanceStaff> findByMobile(String mobile);

    boolean existsByEmail(String email);

    boolean existsByMobile(String mobile);
}
