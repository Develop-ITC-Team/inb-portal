package gov.kh.moi.inb.controller.admin;

import gov.kh.moi.inb.dto.request.ArticleRequestDTO;
import gov.kh.moi.inb.dto.response.ApiResponse;
import gov.kh.moi.inb.dto.response.ArticleResponseDTO;
import gov.kh.moi.inb.dto.response.ArticleSummaryDTO;
import gov.kh.moi.inb.dto.response.PageResponse;
import gov.kh.moi.inb.entity.ArticleStatus;
import gov.kh.moi.inb.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Admin - Articles", description = "JWT-protected article management (CRUD + publish workflow) for the Back Office portal")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/articles")
@RequiredArgsConstructor
public class AdminArticleController {

    private final ArticleService articleService;

    @Operation(summary = "Search/list articles of any status, with optional filters")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<PageResponse<ArticleSummaryDTO>>> search(
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean featured,
            @ParameterObject @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var page = articleService.searchArticles(status, categoryId, search, featured, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Operation(summary = "Get a single article by id, regardless of status")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<ArticleResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getById(id)));
    }

    @Operation(summary = "Create a new article (always starts as DRAFT)")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<ArticleResponseDTO>> create(@Valid @RequestBody ArticleRequestDTO request,
                                                                    Authentication authentication) {
        ArticleResponseDTO created = articleService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Article created successfully", created));
    }

    @Operation(summary = "Update an existing article's content/metadata")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<ArticleResponseDTO>> update(@PathVariable Long id,
                                                                    @Valid @RequestBody ArticleRequestDTO request,
                                                                    Authentication authentication) {
        ArticleResponseDTO updated = articleService.update(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Article updated successfully", updated));
    }

    @Operation(summary = "Transition an article's editorial status (DRAFT -> PUBLISHED -> ARCHIVED)")
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<ArticleResponseDTO>> changeStatus(@PathVariable Long id,
                                                                          @RequestParam ArticleStatus status,
                                                                          Authentication authentication) {
        ArticleResponseDTO updated = articleService.changeStatus(id, status, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Article status updated to " + status, updated));
    }

    @Operation(summary = "Soft-delete an article")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication authentication) {
        articleService.delete(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Article deleted successfully", null));
    }
}
