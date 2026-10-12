package com.example.student_management_system.appointment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select a from Appointment a
            where a.studentId = :studentId
            order by a.slot.slotDate desc, a.slot.startTime desc
            """)
    List<Appointment> findForStudent(@Param("studentId") Long studentId);

    @Query("""
            select a from Appointment a
            where a.slot.leadId = :leadId
            order by a.slot.slotDate desc, a.slot.startTime desc
            """)
    List<Appointment> findForLead(@Param("leadId") Long leadId);

    List<Appointment> findBySlotIdInAndStatus(Collection<Long> slotIds, AppointmentStatus status);

    Optional<Appointment> findFirstBySlotIdAndStatus(Long slotId, AppointmentStatus status);
}
