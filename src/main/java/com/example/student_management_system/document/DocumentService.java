package com.example.student_management_system.document;

import com.example.student_management_system.common.ApiException;
import com.example.student_management_system.document.dto.ChecklistItem;
import com.example.student_management_system.document.dto.DocumentChecklist;
import com.example.student_management_system.document.dto.DocumentResponse;
import com.example.student_management_system.security.AuthUser;
import com.example.student_management_system.student.StudentAccess;
import com.example.student_management_system.student.StudentCategory;
import com.example.student_management_system.student.StudentProfile;
import com.example.student_management_system.student.StudentProfileRepository;
import com.example.student_management_system.user.AppUser;
import com.example.student_management_system.user.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Upload, list, download and delete documents, plus the required-document checklist. */
@Service
public class DocumentService {

    private final StudentDocumentRepository documentRepository;
    private final DocumentTypeRepository typeRepository;
    private final StudentProfileRepository profileRepository;
    private final FileStorageService storage;
    private final StudentAccess access;
    private final long maxUploadBytes;

    public DocumentService(StudentDocumentRepository documentRepository,
                           DocumentTypeRepository typeRepository,
                           StudentProfileRepository profileRepository,
                           FileStorageService storage,
                           StudentAccess access,
                           @Value("${app.max-upload-bytes}") long maxUploadBytes) {
        this.documentRepository = documentRepository;
        this.typeRepository = typeRepository;
        this.profileRepository = profileRepository;
        this.storage = storage;
        this.access = access;
        this.maxUploadBytes = maxUploadBytes;
    }

    /** The checklist of one student (the student themself, or any Lead). */
    @Transactional(readOnly = true)
    public DocumentChecklist checklist(AuthUser me, Long studentId) {
        AppUser student = access.requireViewable(me, studentId);
        StudentCategory category = categoryOf(studentId);

        Map<Long, StudentDocument> uploadedByType = documentRepository.findByStudentIdOrderByUploadedAtDesc(studentId)
                .stream()
                .collect(Collectors.toMap(d -> d.getDocumentType().getId(), Function.identity()));

        Map<Long, DocumentStatus> statusByType = uploadedByType.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getStatus()));
        DocumentCompletion completion = DocumentCompletion.of(
                typeRepository.findByActiveTrueOrderBySortOrderAscIdAsc(), category, statusByType);

        List<ChecklistItem> items = completion.requiredTypes().stream()
                .map(t -> {
                    StudentDocument doc = uploadedByType.get(t.getId());
                    return new ChecklistItem(t.getId(), t.getCode(), t.getName(), t.getDescription(), t.getFormUrl(),
                            doc != null, doc == null ? null : DocumentResponse.from(doc));
                })
                .toList();

        return new DocumentChecklist(student.getId(), student.getFullName(), category,
                completion.requiredCount(), completion.uploadedCount(), completion.verifiedCount(),
                completion.correctionTypes().size(), completion.complete(), completion.allVerified(),
                completion.percent(), items);
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> myDocuments(AuthUser me) {
        return documentRepository.findByStudentIdOrderByUploadedAtDesc(me.getId()).stream()
                .map(DocumentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse get(AuthUser me, Long documentId) {
        return DocumentResponse.from(findViewable(me, documentId));
    }

    /** Upload a file for a document type. If one already exists for that type it is replaced. */
    @Transactional
    public DocumentResponse upload(AuthUser me, Long documentTypeId, MultipartFile file) {
        if (me.getRole() != Role.STUDENT) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only students can upload documents");
        }
        DocumentType type = typeRepository.findById(documentTypeId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document type not found"));
        if (!type.isRequiredFor(categoryOf(me.getId()))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This document type is not required for you");
        }

        // ---- validate the file ----
        if (file == null || file.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Please choose a file to upload");
        }
        if (file.getSize() > maxUploadBytes) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "File is too large (max " + (maxUploadBytes / (1024 * 1024)) + " MB)");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Could not read the uploaded file");
        }
        String originalName = cleanFilename(file.getOriginalFilename());
        AllowedFileType fileType = AllowedFileType.detect(originalName, content)
                .orElseThrow(() -> new ApiException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Only PDF, JPG and PNG files are allowed"));

        // ---- store the file, then create or update the row ----
        String storedName = storage.save(content, fileType.storedExtension());
        StudentDocument doc = documentRepository.findByStudentIdAndDocumentTypeId(me.getId(), type.getId())
                .orElseGet(() -> new StudentDocument(me.getId(), type));
        String oldStoredName = doc.getStoredFilename();   // null for a first upload

        doc.attachFile(originalName, storedName, fileType.contentType(), content.length);
        try {
            doc = documentRepository.saveAndFlush(doc);
        } catch (RuntimeException e) {
            storage.delete(storedName);   // don't leave an orphan file if the database save failed
            throw e;
        }
        if (oldStoredName != null) {
            storage.delete(oldStoredName);  // replacement: remove the previous file
        }
        return DocumentResponse.from(doc);
    }

    @Transactional
    public void delete(AuthUser me, Long documentId) {
        StudentDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
        if (me.getRole() != Role.STUDENT || !doc.getStudentId().equals(me.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only delete your own documents");
        }
        documentRepository.delete(doc);
        documentRepository.flush();
        storage.delete(doc.getStoredFilename());
    }

    /** For GET /api/documents/{id}/file: owner or Lead only. */
    @Transactional(readOnly = true)
    public DownloadableFile download(AuthUser me, Long documentId) {
        StudentDocument doc = findViewable(me, documentId);
        Resource resource = storage.load(doc.getStoredFilename());
        return new DownloadableFile(resource, doc.getOriginalFilename(), doc.getContentType(), doc.getSizeBytes());
    }

    public record DownloadableFile(Resource resource, String filename, String contentType, long sizeBytes) {}

    // ---------- helpers ----------

    private StudentDocument findViewable(AuthUser me, Long documentId) {
        StudentDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Document not found"));
        if (me.getRole() != Role.LEAD && !doc.getStudentId().equals(me.getId())) {
            // Same message as "not found" so students can't probe which ids exist.
            throw new ApiException(HttpStatus.NOT_FOUND, "Document not found");
        }
        return doc;
    }

    private StudentCategory categoryOf(Long studentId) {
        return profileRepository.findByUserId(studentId).map(StudentProfile::getStudentCategory).orElse(null);
    }

    /** Keep only the file name part, drop odd characters, limit the length. */
    static String cleanFilename(String name) {
        if (name == null || name.isBlank()) return "document";
        String base = name.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1);
        base = base.replaceAll("[\\p{Cntrl}\"]", "").trim();
        if (base.isEmpty()) return "document";
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }

    /** Used by the Lead dashboard: student id -> (document type id -> status of that upload). */
    @Transactional(readOnly = true)
    public Map<Long, Map<Long, DocumentStatus>> documentStatusesByStudent() {
        return documentRepository.findAll().stream()
                .collect(Collectors.groupingBy(StudentDocument::getStudentId,
                        Collectors.toMap(d -> d.getDocumentType().getId(), StudentDocument::getStatus)));
    }
}
