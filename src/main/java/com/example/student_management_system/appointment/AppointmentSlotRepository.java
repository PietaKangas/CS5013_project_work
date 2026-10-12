package com.example.student_management_system.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentSlotRepository extends JpaRepository<AppointmentSlot, Long> {

    List<AppointmentSlot> findByLeadIdOrderBySlotDateAscStartTimeAsc(Long leadId);

    /** Number of this Lead's non-cancelled slots on that date that overlap [startTime, endTime). */
    @Query("""
            select count(s) from AppointmentSlot s
            where s.leadId = :leadId
              and s.slotDate = :date
              and s.status <> :cancelled
              and s.startTime < :endTime
              and s.endTime > :startTime
            """)
    long countOverlapping(@Param("leadId") Long leadId,
                          @Param("date") LocalDate date,
                          @Param("startTime") LocalTime startTime,
                          @Param("endTime") LocalTime endTime,
                          @Param("cancelled") SlotStatus cancelled);

    /** Slots with the given status that start after "now". */
    @Query("""
            select s from AppointmentSlot s
            where s.status = :status
              and (s.slotDate > :today or (s.slotDate = :today and s.startTime > :nowTime))
            order by s.slotDate, s.startTime
            """)
    List<AppointmentSlot> findUpcomingByStatus(@Param("status") SlotStatus status,
                                               @Param("today") LocalDate today,
                                               @Param("nowTime") LocalTime nowTime);
}
