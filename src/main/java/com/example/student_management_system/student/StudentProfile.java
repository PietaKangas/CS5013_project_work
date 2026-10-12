package com.example.student_management_system.student;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Objects;

/**
 * A student's profile. Linked to the login account by userId (a plain foreign-key
 * column, which keeps the mapping simple). Full name and email live in "users".
 *
 * The International Lead reviews two parts separately:
 *  - the profile (personal + academic details)  -> profileStatus / profileComment
 *  - the hostel and room details                 -> hostelReviewStatus / hostelComment
 */
@Entity
@Table(name = "student_profiles")
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Set once when the profile is created; never changed by updates.
    @Column(name = "user_id", nullable = false, unique = true, updatable = false)
    private Long userId;

    // ---------- personal / contact ----------
    private String phone;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String nationality;
    private String address;

    @Column(name = "emergency_contact")
    private String emergencyContact;

    // ---------- passport / visa / travel ----------
    @Column(name = "passport_number")
    private String passportNumber;

    @Column(name = "passport_expiry")
    private LocalDate passportExpiry;

    @Column(name = "visa_expiry")
    private LocalDate visaExpiry;

    @Column(name = "arrival_date")
    private LocalDate arrivalDate;

    @Column(name = "departure_date")
    private LocalDate departureDate;

    // ---------- academic ----------
    @Column(name = "home_university")
    private String homeUniversity;

    @Column(name = "student_number", unique = true)
    private String studentNumber;

    private String programme;
    private String department;

    @Column(name = "academic_year")
    private String academicYear;

    @Enumerated(EnumType.STRING)
    @Column(name = "student_category")
    private StudentCategory studentCategory;

    // ---------- hostel ----------
    @Column(name = "hostel_name")
    private String hostelName;

    @Column(name = "room_number")
    private String roomNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "hostel_status", nullable = false)
    private HostelStatus hostelStatus = HostelStatus.NOT_APPLIED;

    // ---------- Lead review of the profile ----------
    @Enumerated(EnumType.STRING)
    @Column(name = "profile_status", nullable = false)
    private ReviewStatus profileStatus = ReviewStatus.PENDING;

    @Column(name = "profile_comment")
    private String profileComment;

    @Column(name = "profile_reviewed_at")
    private Instant profileReviewedAt;

    @Column(name = "profile_reviewed_by")
    private Long profileReviewedBy;

    // ---------- Lead review of the hostel details ----------
    @Enumerated(EnumType.STRING)
    @Column(name = "hostel_review_status", nullable = false)
    private ReviewStatus hostelReviewStatus = ReviewStatus.PENDING;

    @Column(name = "hostel_comment")
    private String hostelComment;

    @Column(name = "hostel_reviewed_at")
    private Instant hostelReviewedAt;

    @Column(name = "hostel_reviewed_by")
    private Long hostelReviewedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StudentProfile() {
        // required by JPA
    }

    public StudentProfile(Long userId) {
        this.userId = userId;
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    // ---------- review helpers ----------

    /** The student changed their details: they must be reviewed again. */
    public void markProfileChanged() {
        this.profileStatus = ReviewStatus.PENDING;
        this.profileComment = null;
        this.profileReviewedAt = null;
        this.profileReviewedBy = null;
    }

    public void reviewProfile(ReviewStatus status, String comment, Long leadId) {
        this.profileStatus = status;
        this.profileComment = comment;
        this.profileReviewedAt = Instant.now();
        this.profileReviewedBy = leadId;
    }

    public void markHostelChanged() {
        this.hostelReviewStatus = ReviewStatus.PENDING;
        this.hostelComment = null;
        this.hostelReviewedAt = null;
        this.hostelReviewedBy = null;
    }

    public void reviewHostel(ReviewStatus status, String comment, Long leadId) {
        this.hostelReviewStatus = status;
        this.hostelComment = comment;
        this.hostelReviewedAt = Instant.now();
        this.hostelReviewedBy = leadId;
    }

    /** All reviewable profile values in one list, used to detect whether a save really changed anything. */
    public int profileFingerprint() {
        return Arrays.hashCode(new Object[]{phone, dateOfBirth, gender, nationality, address, emergencyContact,
                passportNumber, passportExpiry, visaExpiry, arrivalDate, departureDate, homeUniversity,
                studentNumber, programme, department, academicYear, studentCategory});
    }

    public boolean hostelEquals(String name, String room, HostelStatus status) {
        return Objects.equals(hostelName, name) && Objects.equals(roomNumber, room) && hostelStatus == status;
    }

    // ---------- getters ----------
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getPhone() { return phone; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public Gender getGender() { return gender; }
    public String getNationality() { return nationality; }
    public String getAddress() { return address; }
    public String getEmergencyContact() { return emergencyContact; }
    public String getPassportNumber() { return passportNumber; }
    public LocalDate getPassportExpiry() { return passportExpiry; }
    public LocalDate getVisaExpiry() { return visaExpiry; }
    public LocalDate getArrivalDate() { return arrivalDate; }
    public LocalDate getDepartureDate() { return departureDate; }
    public String getHomeUniversity() { return homeUniversity; }
    public String getStudentNumber() { return studentNumber; }
    public String getProgramme() { return programme; }
    public String getDepartment() { return department; }
    public String getAcademicYear() { return academicYear; }
    public StudentCategory getStudentCategory() { return studentCategory; }
    public String getHostelName() { return hostelName; }
    public String getRoomNumber() { return roomNumber; }
    public HostelStatus getHostelStatus() { return hostelStatus; }
    public ReviewStatus getProfileStatus() { return profileStatus; }
    public String getProfileComment() { return profileComment; }
    public Instant getProfileReviewedAt() { return profileReviewedAt; }
    public Long getProfileReviewedBy() { return profileReviewedBy; }
    public ReviewStatus getHostelReviewStatus() { return hostelReviewStatus; }
    public String getHostelComment() { return hostelComment; }
    public Instant getHostelReviewedAt() { return hostelReviewedAt; }
    public Long getHostelReviewedBy() { return hostelReviewedBy; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ---------- setters ----------
    public void setPhone(String phone) { this.phone = phone; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }
    public void setGender(Gender gender) { this.gender = gender; }
    public void setNationality(String nationality) { this.nationality = nationality; }
    public void setAddress(String address) { this.address = address; }
    public void setEmergencyContact(String emergencyContact) { this.emergencyContact = emergencyContact; }
    public void setPassportNumber(String passportNumber) { this.passportNumber = passportNumber; }
    public void setPassportExpiry(LocalDate passportExpiry) { this.passportExpiry = passportExpiry; }
    public void setVisaExpiry(LocalDate visaExpiry) { this.visaExpiry = visaExpiry; }
    public void setArrivalDate(LocalDate arrivalDate) { this.arrivalDate = arrivalDate; }
    public void setDepartureDate(LocalDate departureDate) { this.departureDate = departureDate; }
    public void setHomeUniversity(String homeUniversity) { this.homeUniversity = homeUniversity; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }
    public void setProgramme(String programme) { this.programme = programme; }
    public void setDepartment(String department) { this.department = department; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public void setStudentCategory(StudentCategory studentCategory) { this.studentCategory = studentCategory; }
    public void setHostelName(String hostelName) { this.hostelName = hostelName; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }
    public void setHostelStatus(HostelStatus hostelStatus) { this.hostelStatus = hostelStatus; }
}
