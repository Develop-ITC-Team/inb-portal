package gov.kh.moi.inb.controller.admin;

import gov.kh.moi.inb.dto.response.ApiResponse;
import gov.kh.moi.inb.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Handles raw file uploads independently from article creation, so the frontend can upload
 * images as the editor selects them (e.g. drag-and-drop into a gallery widget) and only send
 * the resulting URLs when the article form is finally submitted via
 * {@code POST /api/admin/articles}.
 */
@Tag(name = "Admin - Media", description = "JWT-protected file upload, used to populate article gallery URLs")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/media")
@RequiredArgsConstructor
public class AdminMediaController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "Upload a single image/video/document and receive back its public URL")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("File uploaded successfully", Map.of("fileUrl", url)));
    }
}
