package com.example.student_management_system.demo;

import com.example.student_management_system.appointment.Appointment;
import com.example.student_management_system.appointment.AppointmentRepository;
import com.example.student_management_system.appointment.AppointmentSlot;
import com.example.student_management_system.appointment.AppointmentSlotRepository;
import com.example.student_management_system.appointment.SlotStatus;
import com.example.student_management_system.document.AllowedFileType;
import com.example.student_management_system.document.DocumentStatus;
import com.example.student_management_system.document.DocumentType;
import com.example.student_management_system.document.DocumentTypeRepository;
import com.example.student_management_system.document.FileStorageService;
import com.example.student_management_system.document.StudentDocument;
import com.example.student_management_system.document.StudentDocumentRepository;
import com.example.student_management_system.student.Gender;
import com.example.student_management_system.student.HostelStatus;
import com.example.student_management_system.student.ReviewStatus;
import com.example.student_management_system.student.StudentCategory;
import com.example.student_management_system.student.StudentProfile;
import com.example.student_management_system.student.StudentProfileRepository;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import com.example.student_management_system.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads FICTIONAL demo students so the system can be tried without any real student data
 * (real documents can't be used for privacy reasons).
 *
 * Turned on with the environment variable DEMO_DATA=true. Runs once: if the first demo
 * student already exists, it does nothing. All "documents" are the watermarked SAMPLE PDFs
 * in src/main/resources/demo-documents/. Demo students log in with DEMO_PASSWORD (default Student123).
 */
@Component
@Order(2)   // after LeadBootstrap, so the demo slots can belong to the bootstrap Lead
public class DemoDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final boolean enabled;
    private final String demoPassword;
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final DocumentTypeRepository typeRepository;
    private final StudentDocumentRepository documentRepository;
    private final FileStorageService storage;
    private final AppointmentSlotRepository slotRepository;
    private final AppointmentRepository appointmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DemoDataLoader(@Value("${app.demo-data.enabled}") boolean enabled,
                          @Value("${app.demo-data.password}") String demoPassword,
                          UserRepository userRepository,
                          StudentProfileRepository profileRepository,
                          DocumentTypeRepository typeRepository,
                          StudentDocumentRepository documentRepository,
                          FileStorageService storage,
                          AppointmentSlotRepository slotRepository,
                          AppointmentRepository appointmentRepository,
                          PasswordEncoder passwordEncoder) {
        this.enabled = enabled;
        this.demoPassword = demoPassword;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.typeRepository = typeRepository;
        this.documentRepository = documentRepository;
        this.storage = storage;
        this.slotRepository = slotRepository;
        this.appointmentRepository = appointmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ------------------------------------------------------------------ demo students

    /** Everything about one fictional student. docs: document code -> status (missing = not uploaded). */
    private record Demo(String name, String email, Gender gender, String nationality, String homeUniversity,
                        StudentCategory category, String department, String programme, String rollNumber,
                        boolean profileFilled, ReviewStatus profileStatus, String profileComment,
                        HostelStatus hostelStatus, String hostel, String room,
                        ReviewStatus hostelReview, String hostelComment,
                        Map<String, DocumentStatus> docs, Map<String, String> docComments) {}

    private static final List<String> COMMON = List.of("ADMISSION_LETTER", "PASSPORT", "VISA", "BONAFIDE_LETTER",
            "BIODATA_FORM", "SELF_DECLARATION", "SOLEMN_AFFIRMATION", "BANK_DETAILS", "PHOTO", "HEALTH_INSURANCE");
    private static final List<String> EXCHANGE_ONLY = List.of("NOMINATION_LETTER", "LEARNING_AGREEMENT");

    private static Map<String, DocumentStatus> all(StudentCategory category, DocumentStatus status) {
        Map<String, DocumentStatus> m = new HashMap<>();
        COMMON.forEach(c -> m.put(c, status));
        if (category == StudentCategory.EXCHANGE) EXCHANGE_ONLY.forEach(c -> m.put(c, status));
        else m.put("DEGREE_CERTIFICATE", status);
        return m;
    }

    private static Map<String, DocumentStatus> with(Map<String, DocumentStatus> base, Object... codeStatusPairs) {
        Map<String, DocumentStatus> m = new HashMap<>(base);
        for (int i = 0; i < codeStatusPairs.length; i += 2) {
            String code = (String) codeStatusPairs[i];
            DocumentStatus status = (DocumentStatus) codeStatusPairs[i + 1];
            if (status == null) m.remove(code); else m.put(code, status);
        }
        return m;
    }

    private static List<Demo> students() {
        StudentCategory ex = StudentCategory.EXCHANGE;
        DocumentStatus up = DocumentStatus.UPLOADED, ok = DocumentStatus.VERIFIED, fix = DocumentStatus.NEEDS_CORRECTION;
        List<Demo> list = new ArrayList<>();

        // 1. Everything verified
        list.add(new Demo("Elina Virtanen", "elina.virtanen@example.com", Gender.FEMALE, "Finnish", "Aalto University",
                ex, "Computer Science and Engineering", "Semester exchange – B.Tech", "EX26F001",
                true, ReviewStatus.VERIFIED, null, HostelStatus.ALLOCATED, "Sabarmati Hostel", "214",
                ReviewStatus.VERIFIED, null, all(ex, ok), Map.of()));

        // 2. Submitted everything, nothing reviewed yet
        list.add(new Demo("Lukas Becker", "lukas.becker@example.com", Gender.MALE, "German", "Technical University of Munich",
                ex, "Mechanical Engineering", "Semester exchange – M.Tech", "EX26F002",
                true, ReviewStatus.PENDING, null, HostelStatus.ALLOCATED, "Tamiraparani Hostel", "118",
                ReviewStatus.PENDING, null, all(ex, up), Map.of()));

        // 3. Passport scan sent back for correction
        list.add(new Demo("Sofía Martínez", "sofia.martinez@example.com", Gender.FEMALE, "Spanish",
                "Universidad Politécnica de Madrid", ex, "Civil Engineering", "Semester exchange – B.Tech", "EX26F003",
                true, ReviewStatus.VERIFIED, null, HostelStatus.APPLIED, null, null, ReviewStatus.PENDING, null,
                with(all(ex, ok), "PASSPORT", fix),
                Map.of("PASSPORT", "The scan is cut off at the bottom. Please upload the full photo page, "
                        + "including the two machine-readable lines.")));

        // 4. Halfway: profile not finished, three documents waiting for review
        list.add(new Demo("Kenji Tanaka", "kenji.tanaka@example.com", Gender.MALE, "Japanese", "Tohoku University",
                ex, "Electrical Engineering", "Semester exchange – B.Tech", "EX26F004",
                false, ReviewStatus.PENDING, null, HostelStatus.NOT_APPLIED, null, null, ReviewStatus.PENDING, null,
                Map.of("ADMISSION_LETTER", up, "PASSPORT", up, "VISA", up), Map.of()));

        // 5. Degree student whose profile needs a correction
        list.add(new Demo("Amara Okafor", "amara.okafor@example.com", Gender.FEMALE, "Nigerian", "University of Lagos",
                StudentCategory.DEGREE, "Engineering Design", "MS (by Research)", "ED26S005",
                true, ReviewStatus.NEEDS_CORRECTION,
                "Your roll number doesn't match the one on your IIT Madras offer letter. Please check and update it.",
                HostelStatus.ALLOCATED, "Sabarmati Hostel", "305", ReviewStatus.VERIFIED, null,
                with(all(StudentCategory.DEGREE, ok), "BANK_DETAILS", null, "HEALTH_INSURANCE", null,
                        "DEGREE_CERTIFICATE", up), Map.of()));

        // 6. Just signed up, nothing filled in
        list.add(new Demo("Lucas Moreau", "lucas.moreau@example.com", null, null, null,
                null, null, null, null, false, ReviewStatus.PENDING, null,
                HostelStatus.NOT_APPLIED, null, null, ReviewStatus.PENDING, null, Map.of(), Map.of()));

        // 7. All uploaded, about half reviewed
        list.add(new Demo("Emma Johansson", "emma.johansson@example.com", Gender.FEMALE, "Swedish",
                "KTH Royal Institute of Technology", ex, "Management Studies", "Semester exchange – MBA", "EX26F007",
                true, ReviewStatus.VERIFIED, null, HostelStatus.ALLOCATED, "Sabarmati Hostel", "122",
                ReviewStatus.VERIFIED, null,
                with(all(ex, ok), "BANK_DETAILS", up, "PHOTO", up, "HEALTH_INSURANCE", up,
                        "NOMINATION_LETTER", up, "LEARNING_AGREEMENT", up), Map.of()));

        // 8. Bank proof and hostel room sent back for correction
        list.add(new Demo("Matteo Rossi", "matteo.rossi@example.com", Gender.MALE, "Italian", "Politecnico di Milano",
                ex, "Computer Science and Engineering", "Semester exchange – M.Tech", "EX26F008",
                true, ReviewStatus.VERIFIED, null, HostelStatus.ALLOCATED, "Tamiraparani Hostel", "207",
                ReviewStatus.NEEDS_CORRECTION,
                "The hostel office has you in room 209, not 207. Please check your allotment slip and update it.",
                with(all(ex, ok), "BANK_DETAILS", fix),
                Map.of("BANK_DETAILS", "The account number isn't visible. Please upload a statement or "
                        + "passbook page that shows it.")));
        return list;
    }

    // ------------------------------------------------------------------ loading

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        List<Demo> demos = students();
        if (userRepository.existsByEmail(demos.get(0).email())) {
            log.info("Demo data already loaded.");
            return;
        }

        Long leadId = userRepository.findByRole(Role.LEAD).stream().findFirst().map(AppUser::getId).orElse(null);
        Map<String, DocumentType> types = typeRepository.findAll().stream()
                .collect(Collectors.toMap(DocumentType::getCode, Function.identity()));

        int n = 0;
        Map<String, Long> idsByEmail = new HashMap<>();
        for (Demo d : demos) {
            n++;
            AppUser user = userRepository.save(
                    new AppUser(d.name(), d.email(), passwordEncoder.encode(demoPassword), Role.STUDENT));
            idsByEmail.put(d.email(), user.getId());
            if (d.nationality() != null) {
                profileRepository.save(buildProfile(user.getId(), d, n, leadId));
            }
            for (Map.Entry<String, DocumentStatus> e : d.docs().entrySet()) {
                DocumentType type = types.get(e.getKey());
                if (type == null) continue;
                String stored = storeSample(e.getKey());
                if (stored == null) continue;
                StudentDocument doc = new StudentDocument(user.getId(), type);
                doc.attachFile(e.getKey().toLowerCase().replace('_', '-') + "-sample.pdf", stored,
                        AllowedFileType.PDF.contentType(), sampleSize(e.getKey()));
                if (e.getValue() != DocumentStatus.UPLOADED) {
                    doc.review(e.getValue(), d.docComments().get(e.getKey()), leadId);
                }
                documentRepository.save(doc);
            }
        }

        if (leadId != null) {
            createSlots(leadId, idsByEmail.get("lukas.becker@example.com"), idsByEmail.get("sofia.martinez@example.com"));
        } else {
            log.info("No Lead account found, so no demo appointment slots were created.");
        }
        log.info("Loaded {} fictional demo students (password: {}).", demos.size(), demoPassword);
    }

    private StudentProfile buildProfile(Long userId, Demo d, int n, Long leadId) {
        StudentProfile p = new StudentProfile(userId);
        p.setNationality(d.nationality());
        p.setGender(d.gender());
        p.setHomeUniversity(d.homeUniversity());
        p.setStudentCategory(d.category());
        p.setDepartment(d.department());
        p.setProgramme(d.programme());
        p.setStudentNumber(d.rollNumber());
        p.setAcademicYear("July–November 2026");
        p.setPhone(String.format("+91 90000 %05d", n));
        p.setPassportNumber(String.format("DEMO%04d", n));
        p.setVisaExpiry(LocalDate.of(2026, 12, 31));
        if (d.profileFilled()) {
            p.setDateOfBirth(LocalDate.of(2002, 1 + n, 10 + n));
            p.setAddress(d.hostel() != null ? d.hostel() + ", IIT Madras, Chennai 600036" : "IIT Madras Guest House, Chennai 600036");
            p.setEmergencyContact("Parent (fictional) +00 000 000 00" + n);
            p.setPassportExpiry(LocalDate.of(2030 + n % 4, 6, 30));
            p.setArrivalDate(LocalDate.of(2026, 7, 18 + n));
            p.setDepartureDate(LocalDate.of(2026, 11, 28));
        }
        p.setHostelStatus(d.hostelStatus());
        p.setHostelName(d.hostel());
        p.setRoomNumber(d.room());
        if (d.profileStatus() != ReviewStatus.PENDING) p.reviewProfile(d.profileStatus(), d.profileComment(), leadId);
        if (d.hostelReview() != ReviewStatus.PENDING) p.reviewHostel(d.hostelReview(), d.hostelComment(), leadId);
        return p;
    }

    /**
     * Copies the sample PDF into the upload folder. Every demo upload gets its own copy,
     * exactly like a real upload, so deleting or replacing one never affects another student.
     */
    private String storeSample(String code) {
        try (InputStream in = new ClassPathResource("demo-documents/" + code + ".pdf").getInputStream()) {
            return storage.save(in.readAllBytes(), AllowedFileType.PDF.storedExtension());
        } catch (IOException e) {
            log.warn("Sample document for {} not found, skipping it", code);
            return null;
        }
    }

    private static long sampleSize(String code) {
        try (InputStream in = new ClassPathResource("demo-documents/" + code + ".pdf").getInputStream()) {
            return in.readAllBytes().length;
        } catch (IOException e) {
            return 0;
        }
    }

    /** A few 20-minute slots on the next three working days; two of them booked by demo students. */
    private void createSlots(Long leadId, Long bookerA, Long bookerB) {
        LocalDate day = LocalDate.now().plusDays(1);
        int created = 0;
        List<AppointmentSlot> saved = new ArrayList<>();
        while (created < 3) {
            if (day.getDayOfWeek() != DayOfWeek.SATURDAY && day.getDayOfWeek() != DayOfWeek.SUNDAY) {
                for (LocalTime start : List.of(LocalTime.of(10, 0), LocalTime.of(10, 30), LocalTime.of(14, 30))) {
                    LocalTime end = start.plusMinutes(20);
                    if (slotRepository.countOverlapping(leadId, day, start, end, SlotStatus.CANCELLED) == 0) {
                        saved.add(slotRepository.save(new AppointmentSlot(leadId, day, start, end)));
                    }
                }
                created++;
            }
            day = day.plusDays(1);
        }
        bookDemo(saved, 0, bookerA);
        bookDemo(saved, 4, bookerB);
    }

    private void bookDemo(List<AppointmentSlot> slots, int index, Long studentId) {
        if (studentId == null || index >= slots.size()) return;
        AppointmentSlot slot = slots.get(index);
        slot.setStatus(SlotStatus.BOOKED);
        AppointmentSlot booked = slotRepository.save(slot);
        appointmentRepository.save(new Appointment(booked, studentId));
    }
}
