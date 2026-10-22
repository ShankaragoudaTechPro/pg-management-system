package com.pgmanager.pgmanagement.scheduler;

import com.pgmanager.pgmanagement.service.NotificationService;
import com.pgmanager.pgmanagement.service.RentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RentReminderScheduler {

    private final RentService rentService;
    private final NotificationService notificationService;

    /**
     * 1. Generate missing monthly rent records.
     *
     * Runs every day at 12:00 AM.
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void generateMonthlyRents() {

        rentService.generateMonthlyRents();
    }

    /**
     * 2. Mark pending rents as overdue
     *    when their due date has passed.
     *
     * Runs every day at 12:05 AM.
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void updateOverdueRents() {

        rentService.updateOverdueRents();
    }

    /**
     * 3. Generate rent notifications.
     *
     * Runs every day at 12:10 AM.
     */
    @Scheduled(cron = "0 10 0 * * *")
    public void generateRentNotifications() {

        notificationService.generateRentNotifications();
    }
}