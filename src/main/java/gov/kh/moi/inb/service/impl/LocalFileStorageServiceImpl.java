package gov.kh.moi.inb.service.impl;

import gov.kh.moi.inb.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Disk-backed implementation of {@link FileStorageService} for local development / early
 * production. Designed to be a drop-in replacement target: swap this {@code @Service} for a
 * {@code MinioFileStorageServiceImpl} when object storage is introduced, with no changes
 * required anywhere else in the codebase.
 *
 * <p><b>Security note:</b> file-type validation inspects the actual file bytes (via Apache
 * Tika's magic-byte / content-sniffing detection), not just the client-supplied filename
 * extension or declared {@code Content-Type} header - both of which are trivial for a client
 * to spoof (e.g. renaming {@code malware.exe} to {@code photo.jpg}). A file whose real content
 * doesn't match an allowed type is rejected regardless of what it's named.
 */
@Slf4j
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    /** Maps each allowed real (sniffed) MIME type to the extension used when storing the file. */
    private static final Map<String, String> ALLOWED_MIME_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/gif", "gif",
            "image/webp", "webp",
            "video/mp4", "mp4",
            "application/pdf", "pdf"
    );

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp", "mp4", "pdf");
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10MB, mirrors application.yml

    private final Path uploadRoot;
    private final String publicBaseUrl;
    private final Tika tika = new Tika();

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
        String safeExtension = validateAndDetectExtension(file);
        String storedFilename = UUID.randomUUID() + "." + safeExtension;

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

    /**
     * Validates size, extension, AND actual file content (magic bytes) - all three must agree
     * before a file is accepted. Returns the canonical extension to store the file under,
     * derived from the sniffed content type rather than trusting the client's filename.
     */
    private String validateAndDetectExtension(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Uploaded file exceeds the maximum allowed size of 10MB");
        }

        String claimedExtension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (claimedExtension == null || !ALLOWED_EXTENSIONS.contains(claimedExtension.toLowerCase())) {
            throw new IllegalArgumentException("Unsupported file type: " + claimedExtension);
        }

        String detectedMimeType;
        try (InputStream in = file.getInputStream()) {
            detectedMimeType = tika.detect(in, file.getOriginalFilename());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to inspect uploaded file content", e);
        }

        String canonicalExtension = ALLOWED_MIME_TYPES.get(detectedMimeType);
        if (canonicalExtension == null) {
            log.warn("Rejected upload: claimed extension '{}' but actual content detected as '{}'",
                    claimedExtension, detectedMimeType);
            throw new IllegalArgumentException(
                    "File content does not match an allowed type (detected: " + detectedMimeType + ")");
        }

        return canonicalExtension;
    }
}