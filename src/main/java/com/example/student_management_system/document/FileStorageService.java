package com.example.student_management_system.document;

import com.example.student_management_system.common.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

/**
 * Saves uploaded files to a folder on the server (app.upload-dir, default ./uploads).
 * Files get a random UUID name, so a user-supplied name can never point outside
 * the folder (no "../../" tricks) and two files can never clash.
 */
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final Path root;

    public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create upload folder " + root, e);
        }
        log.info("Uploaded documents are stored in {}", root);
    }

    /** Writes the bytes to a new random file and returns its stored name. */
    public String save(byte[] content, String extension) {
        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Files.write(resolve(storedName), content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save uploaded file", e);
        }
        return storedName;
    }

    public Resource load(String storedName) {
        Path path = resolve(storedName);
        if (!Files.isRegularFile(path)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "The file for this document is missing on the server");
        }
        return new FileSystemResource(path);
    }

    /** Best effort: a leftover file is harmless, so we only log failures. */
    public void delete(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException e) {
            log.warn("Could not delete stored file {}", storedName, e);
        }
    }

    private Path resolve(String storedName) {
        Path path = root.resolve(storedName).normalize();
        if (!path.startsWith(root)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid file reference");
        }
        return path;
    }
}
