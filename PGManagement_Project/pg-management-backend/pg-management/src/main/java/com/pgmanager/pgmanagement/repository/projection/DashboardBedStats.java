package com.pgmanager.pgmanagement.repository.projection;

import com.pgmanager.pgmanagement.entity.BedStatus;

public interface DashboardBedStats {
    Long getPgId();
    BedStatus getStatus();
    Long getBedCount();
}
