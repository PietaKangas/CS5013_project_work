package com.example.student_management_system.student;

import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.student.dto.HostelUpdateRequest;
import com.example.student_management_system.student.dto.ProfileResponse;
import com.example.student_management_system.student.dto.ProfileUpdateRequest;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import com.example.student_management_system.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reading and updating student profiles and hostel information. */
@Service
public class ProfileService {

    private final StudentProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final StudentAccess access;

    public ProfileService(StudentProfileRepository profileRepository,
                          UserRepository userRepository,
                          StudentAccess access) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(AuthUser me, Long studentId) {
        AppUser student = access.requireViewable(me, studentId);
        StudentProfile profile = profileRepository.findByUserId(studentId).orElse(null);
        return ProfileResponse.from(student, profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getByStudentNumber(String studentNumber) {
        StudentProfile profile = profileRepository.findByStudentNumberIgnoreCase(studentNumber.trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No student with that roll number"));
        AppUser student = access.requireStudent(profile.getUserId());
        return ProfileResponse.from(student, profile);
    }

    /** Student updates their own personal / academic details. Any real change sends the profile back for review. */
    @Transactional
    public ProfileResponse updateProfile(AuthUser me, Long studentId, ProfileUpdateRequest req) {
        AppUser student = access.requireSelf(me, studentId);

        String studentNumber = trimToNull(req.studentNumber());
        if (studentNumber != null
                && profileRepository.existsByStudentNumberIgnoreCaseAndUserIdNot(studentNumber, studentId)) {
            throw new ApiException(HttpStatus.CONFLICT, "This roll number is already used by another student");
        }
        if (req.passportExpiry() != null && req.arrivalDate() != null
                && !req.passportExpiry().isAfter(req.arrivalDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Passport expiry date must be after your arrival date");
        }
        if (req.arrivalDate() != null && req.departureDate() != null
                && req.departureDate().isBefore(req.arrivalDate())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Departure date can't be before arrival date");
        }

        String newName = req.fullName().trim();
        boolean nameChanged = !newName.equals(student.getFullName());
        student.setFullName(newName);   // name lives on the account; email/role are never touched here
        userRepository.save(student);

        StudentProfile p = profileRepository.findByUserId(studentId).orElseGet(() -> new StudentProfile(studentId));
        int before = p.profileFingerprint();

        p.setPhone(trimToNull(req.phone()));
        p.setDateOfBirth(req.dateOfBirth());
        p.setGender(req.gender());
        p.setNationality(trimToNull(req.nationality()));
        p.setAddress(trimToNull(req.address()));
        p.setEmergencyContact(trimToNull(req.emergencyContact()));
        p.setPassportNumber(upperOrNull(req.passportNumber()));
        p.setPassportExpiry(req.passportExpiry());
        p.setVisaExpiry(req.visaExpiry());
        p.setArrivalDate(req.arrivalDate());
        p.setDepartureDate(req.departureDate());
        p.setHomeUniversity(trimToNull(req.homeUniversity()));
        p.setStudentNumber(studentNumber == null ? null : studentNumber.toUpperCase());
        p.setProgramme(trimToNull(req.programme()));
        p.setDepartment(trimToNull(req.department()));
        p.setAcademicYear(trimToNull(req.academicYear()));
        p.setStudentCategory(req.studentCategory());

        if (nameChanged || p.profileFingerprint() != before) {
            p.markProfileChanged();   // the Lead must check it again
        }
        return ProfileResponse.from(student, profileRepository.saveAndFlush(p));
    }

    /**
     * Hostel details can be changed by the student (then they await review) or by a Lead
     * (then they count as verified straight away).
     */
    @Transactional
    public ProfileResponse updateHostel(AuthUser me, Long studentId, HostelUpdateRequest req) {
        AppUser student = access.requireSelfOrLead(me, studentId);

        String hostelName = trimToNull(req.hostelName());
        String roomNumber = trimToNull(req.roomNumber());
        if (req.hostelStatus() == HostelStatus.ALLOCATED && (hostelName == null || roomNumber == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Hostel name and room number are required when the status is ALLOCATED");
        }

        StudentProfile p = profileRepository.findByUserId(studentId).orElseGet(() -> new StudentProfile(studentId));
        boolean changed = !p.hostelEquals(hostelName, roomNumber, req.hostelStatus());
        p.setHostelName(hostelName);
        p.setRoomNumber(roomNumber);
        p.setHostelStatus(req.hostelStatus());

        if (me.getRole() == Role.LEAD) {
            p.reviewHostel(ReviewStatus.VERIFIED, null, me.getId());
        } else if (changed) {
            p.markHostelChanged();
        }
        return ProfileResponse.from(student, profileRepository.saveAndFlush(p));
    }

    static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String upperOrNull(String s) {
        String t = trimToNull(s);
        return t == null ? null : t.toUpperCase();
    }
}
