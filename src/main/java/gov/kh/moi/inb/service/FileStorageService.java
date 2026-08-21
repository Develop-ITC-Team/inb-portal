package gov.kh.moi.inb.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction over "where uploaded files physically live". Local disk today
 * ({@code LocalFileStorageServiceImpl}); swap in a {@code MinioFileStorageServiceImpl} later
 * with zero changes to any controller or service that depends on this interface -
 * Dependency Inversion Principle in practice.
 */
public interface FileStorageService {

    /** Stores the file and returns a publicly-resolvable URL. */
    String store(MultipartFile file);

    void delete(String fileUrl);
}
