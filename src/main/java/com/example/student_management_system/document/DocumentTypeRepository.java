package com.example.student_management_system.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {

    List<DocumentType> findByActiveTrueOrderBySortOrderAscIdAsc();

    List<DocumentType> findAllByOrderBySortOrderAscIdAsc();

    boolean existsByCodeIgnoreCase(String code);
}
