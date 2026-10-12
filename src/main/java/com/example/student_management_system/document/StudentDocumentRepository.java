package com.example.student_management_system.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudentIdOrderByUploadedAtDesc(Long studentId);

    Optional<StudentDocument> findByStudentIdAndDocumentTypeId(Long studentId, Long documentTypeId);
}
