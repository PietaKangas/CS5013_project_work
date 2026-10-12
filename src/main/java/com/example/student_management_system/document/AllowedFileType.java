package com.example.student_management_system.document;

import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * The only file types students may upload. We check BOTH the file extension and the
 * first bytes of the file ("magic number"), so renaming virus.exe to virus.pdf
 * doesn't get past the check.
 */
public enum AllowedFileType {

    PDF("application/pdf", "pdf", Set.of("pdf"), new int[]{0x25, 0x50, 0x44, 0x46}),          // "%PDF"
    PNG("image/png", "png", Set.of("png"), new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}),
    JPEG("image/jpeg", "jpg", Set.of("jpg", "jpeg"), new int[]{0xFF, 0xD8, 0xFF});

    private final String contentType;
    private final String storedExtension;
    private final Set<String> extensions;
    private final int[] magic;

    AllowedFileType(String contentType, String storedExtension, Set<String> extensions, int[] magic) {
        this.contentType = contentType;
        this.storedExtension = storedExtension;
        this.extensions = extensions;
        this.magic = magic;
    }

    public String contentType() { return contentType; }
    public String storedExtension() { return storedExtension; }

    /** Returns the type only if the extension AND the file content agree. */
    public static Optional<AllowedFileType> detect(String filename, byte[] content) {
        String ext = extensionOf(filename);
        for (AllowedFileType type : values()) {
            if (type.extensions.contains(ext) && type.startsWithMagic(content)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }

    private boolean startsWithMagic(byte[] content) {
        if (content == null || content.length < magic.length) return false;
        for (int i = 0; i < magic.length; i++) {
            if ((content[i] & 0xFF) != magic[i]) return false;
        }
        return true;
    }

    static String extensionOf(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
