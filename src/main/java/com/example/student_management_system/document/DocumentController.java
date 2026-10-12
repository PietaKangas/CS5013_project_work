package com.example.student_management_system.document;

import com.example.student_management_system.document.dto.DocumentChecklist;
import com.example.student_management_system.document.dto.DocumentResponse;
import com.example.student_management_system.security.AuthUser;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Student:
 *   GET    /api/documents/required        my checklist (required types + what I've uploaded)
 *   GET    /api/documents/my              my uploaded documents
 *   POST   /api/documents                 upload or replace (multipart: documentTypeId, file)
 *   DELETE /api/documents/{id}            delete my document
 * Student (own) or Lead:
 *   GET    /api/documents/{id}            metadata
 *   GET    /api/documents/{id}/file       the file itself (?inline=true to view in browser)
 *   GET    /api/students/{id}/documents   a student's checklist (Leads: any student)
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping("/documents/required")
    @PreAuthorize("hasRole('STUDENT')")
    public DocumentChecklist myChecklist(@AuthenticationPrincipal AuthUser me) {
        return documentService.checklist(me, me.getId());
    }

    @GetMapping("/documents/my")
    @PreAuthorize("hasRole('STUDENT')")
    public List<DocumentResponse> myDocuments(@AuthenticationPrincipal AuthUser me) {
        return documentService.myDocuments(me);
    }

    @PostMapping(value = "/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public DocumentResponse upload(@AuthenticationPrincipal AuthUser me,
                                   @RequestParam("documentTypeId") Long documentTypeId,
                                   @RequestParam("file") MultipartFile file) {
        return documentService.upload(me, documentTypeId, file);
    }

    @GetMapping("/documents/{id}")
    public DocumentResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return documentService.get(me, id);
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<Resource> file(@AuthenticationPrincipal AuthUser me,
                                         @PathVariable Long id,
                                         @RequestParam(defaultValue = "false") boolean inline) {
        DocumentService.DownloadableFile f = documentService.download(me, id);
        ContentDisposition disposition = (inline ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(f.filename(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(f.contentType()))
                .contentLength(f.sizeBytes())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(f.resource());
    }

    @DeleteMapping("/documents/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        documentService.delete(me, id);
    }

    @GetMapping("/students/{studentId}/documents")
    public DocumentChecklist studentChecklist(@AuthenticationPrincipal AuthUser me, @PathVariable Long studentId) {
        return documentService.checklist(me, studentId);
    }
}
