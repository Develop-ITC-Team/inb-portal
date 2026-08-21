package gov.kh.moi.inb.service.impl;

import gov.kh.moi.inb.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/**
 * Disk-backed implementation of {@link FileStorageService} for local development / early
 * production. Designed to be a drop-in replacement target: swap this {@code @Service} for a
 * {@code MinioFileStorageServiceImpl} when object storage is introduced, with no changes
 * required anywhere else in the codebase.
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "pdf");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB, mirrors application.yml

    private final Path uploadRoot;
    private final String publicBaseUrl;

    public LocalFileStorageServiceImpl(@Value("${app.file-storage.upload-dir}") String uploadDir,
                                        @Value("${app.file-storage.public-base-url}") String publicBaseUrl) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl;
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload directory: " + uploadRoot, e);
        }
    }

    @Override
    public String store(MultipartFile file) {
        validate(file);

        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID() + (extension != null ? "." + extension.toLowerCase() : "");

        // Path traversal guard: resolve then verify the result is still inside uploadRoot.
        Path target = uploadRoot.resolve(storedFilename).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid file name");
        }

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store uploaded file", e);
            throw new IllegalStateException("Failed to store uploaded file", e);
        }

        return publicBaseUrl + "/" + storedFilename;
    }

    @Override
    public void delete(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(publicBaseUrl)) {
            return; // Not a file managed by this store (e.g. external URL) - nothing to do.
        }
        String filename = fileUrl.substring(publicBaseUrl.length() + 1);
        Path target = uploadRoot.resolve(filename).normalize();
        if (!target.startsWith(uploadRoot)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            log.warn("Failed to delete file: {}", target, e);
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Uploaded file exceeds the maximum allowed size of 10MB");
        }
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (extension == null || !ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + extension);
        }
    }
}
