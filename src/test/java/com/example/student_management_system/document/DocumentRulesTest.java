package com.example.student_management_system.document;

import com.example.student_management_system.student.StudentCategory;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Plain unit tests (no database needed) for file-type checks and document completion. */
class DocumentRulesTest {

    private static final byte[] PDF = "%PDF-1.7 rest of file".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

    @Test
    void acceptsRealPdfPngAndJpeg() {
        assertEquals(AllowedFileType.PDF, AllowedFileType.detect("passport.PDF", PDF).orElseThrow());
        assertEquals(AllowedFileType.PNG, AllowedFileType.detect("photo.png", PNG).orElseThrow());
        assertEquals(AllowedFileType.JPEG, AllowedFileType.detect("photo.jpeg", JPEG).orElseThrow());
        assertEquals(AllowedFileType.JPEG, AllowedFileType.detect("photo.jpg", JPEG).orElseThrow());
    }

    @Test
    void rejectsRenamedOrUnknownFiles() {
        byte[] exe = {0x4D, 0x5A, 0x00, 0x00};                                // "MZ" = Windows .exe
        assertTrue(AllowedFileType.detect("virus.pdf", exe).isEmpty());       // right name, wrong content
        assertTrue(AllowedFileType.detect("doc.exe", PDF).isEmpty());         // right content, wrong name
        assertTrue(AllowedFileType.detect("photo.png", JPEG).isEmpty());      // extension and content disagree
        assertTrue(AllowedFileType.detect("noextension", PDF).isEmpty());
        assertTrue(AllowedFileType.detect("empty.pdf", new byte[0]).isEmpty());
    }

    @Test
    void cleansUploadedFilenames() {
        assertEquals("passport.pdf", DocumentService.cleanFilename("C:\\Users\\me\\passport.pdf"));
        assertEquals("passport.pdf", DocumentService.cleanFilename("../../etc/passport.pdf"));
        assertEquals("document", DocumentService.cleanFilename("  "));
    }

    @Test
    void completionDependsOnCategoryAndUploads() {
        DocumentType passport = type(1L, "PASSPORT", null);
        DocumentType degree = type(2L, "DEGREE_CERTIFICATE", StudentCategory.DEGREE);
        DocumentType learning = type(3L, "LEARNING_AGREEMENT", StudentCategory.EXCHANGE);
        DocumentType inactive = type(4L, "OLD", null);
        inactive.setActive(false);
        List<DocumentType> all = List.of(passport, degree, learning, inactive);

        DocumentCompletion exchange = DocumentCompletion.of(all, StudentCategory.EXCHANGE,
                Map.of(1L, DocumentStatus.UPLOADED));
        assertEquals(2, exchange.requiredCount());          // passport + learning agreement
        assertEquals(1, exchange.uploadedCount());
        assertEquals(50, exchange.percent());
        assertFalse(exchange.complete());
        assertEquals("LEARNING_AGREEMENT", exchange.missingTypes().get(0).getCode());
        assertEquals(1, exchange.awaitingReviewTypes().size());   // uploaded, not reviewed yet
        assertFalse(exchange.allVerified());

        DocumentCompletion degreeDone = DocumentCompletion.of(all, StudentCategory.DEGREE,
                Map.of(1L, DocumentStatus.VERIFIED, 2L, DocumentStatus.NEEDS_CORRECTION, 3L, DocumentStatus.UPLOADED));
        assertTrue(degreeDone.complete());                     // everything uploaded...
        assertEquals(100, degreeDone.percent());
        assertEquals(1, degreeDone.verifiedCount());
        assertEquals("DEGREE_CERTIFICATE", degreeDone.correctionTypes().get(0).getCode()); // ...but one must be fixed
        assertFalse(degreeDone.allVerified());

        DocumentCompletion degreeVerified = DocumentCompletion.of(all, StudentCategory.DEGREE,
                Map.of(1L, DocumentStatus.VERIFIED, 2L, DocumentStatus.VERIFIED));
        assertTrue(degreeVerified.allVerified());

        DocumentCompletion noCategory = DocumentCompletion.of(all, null, Map.of());
        assertEquals(1, noCategory.requiredCount());        // only the "everyone" type
    }

    private static DocumentType type(Long id, String code, StudentCategory category) {
        DocumentType t = new DocumentType(code, code, null, category, 0);
        try {
            var field = DocumentType.class.getDeclaredField("id");   // id is normally set by the database
            field.setAccessible(true);
            field.set(t, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return t;
    }
}
