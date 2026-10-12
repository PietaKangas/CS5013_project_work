package com.example.student_management_system.document;

import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.document.dto.DocumentTypeRequest;
import com.example.student_management_system.document.dto.DocumentTypeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Lead-only management of the required document list (SecurityConfig: /api/admin/** = LEAD).
 *   GET  /api/admin/document-types         all types, including inactive ones
 *   POST /api/admin/document-types         add a type
 *   PUT  /api/admin/document-types/{id}    edit / activate / deactivate a type
 * Types are never deleted (uploaded documents point to them); deactivate instead.
 * This logic is small, so it lives in the controller instead of a separate service.
 */
@RestController
@RequestMapping("/api/admin/document-types")
public class DocumentTypeController {

    private final DocumentTypeRepository typeRepository;

    public DocumentTypeController(DocumentTypeRepository typeRepository) {
        this.typeRepository = typeRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<DocumentTypeResponse> list() {
        return typeRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(DocumentTypeResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public DocumentTypeResponse create(@Valid @RequestBody DocumentTypeRequest req) {
        if (req.code() == null || req.code().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Code is required");
        }
        String code = req.code().trim().toUpperCase(Locale.ROOT);
        if (typeRepository.existsByCodeIgnoreCase(code)) {
            throw new ApiException(HttpStatus.CONFLICT, "A document type with this code already exists");
        }
        DocumentType type = new DocumentType(code, req.name().trim(), blankToNull(req.description()),
                req.studentCategory(), req.sortOrder() == null ? 100 : req.sortOrder());
        if (req.active() != null) {
            type.setActive(req.active());
        }
        type.setFormUrl(blankToNull(req.formUrl()));
        return DocumentTypeResponse.from(typeRepository.save(type));
    }

    @PutMapping("/{id}")
    @Transactional
    public DocumentTypeResponse update(@PathVariable Long id, @Valid @RequestBody DocumentTypeRequest req) {
        DocumentType type = typeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document type not found"));
        type.setName(req.name().trim());
        type.setDescription(blankToNull(req.description()));
        type.setStudentCategory(req.studentCategory());
        if (req.active() != null) type.setActive(req.active());
        if (req.sortOrder() != null) type.setSortOrder(req.sortOrder());
        type.setFormUrl(blankToNull(req.formUrl()));
        return DocumentTypeResponse.from(typeRepository.save(type));
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
