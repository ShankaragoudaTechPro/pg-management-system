package com.pgmanager.pgmanagement.repository.projection;

public interface DashboardRentStats {
    Long getPgId();
    Double getRentDue();
    Double getRentOverdue();
    Double getRentCollected();
}
