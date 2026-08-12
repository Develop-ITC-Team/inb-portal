package gov.kh.moi.inb.controller.publicapi;

import gov.kh.moi.inb.dto.response.ApiResponse;
import gov.kh.moi.inb.dto.response.ArticleResponseDTO;
import gov.kh.moi.inb.dto.response.ArticleSummaryDTO;
import gov.kh.moi.inb.dto.response.PageResponse;
import gov.kh.moi.inb.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Public - Articles", description = "Unauthenticated read endpoints consumed by the citizen-facing portal. Only PUBLISHED articles are ever returned.")
@RestController
@RequestMapping("/api/public/articles")
@RequiredArgsConstructor
public class PublicArticleController {

    private final ArticleService articleService;

    @Operation(summary = "List published articles, optionally filtered by category, paginated")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleSummaryDTO>>> list(
            @Parameter(description = "Filter by category id") @RequestParam(required = false) Long categoryId,
            @ParameterObject @PageableDefault(size = 12, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var page = articleService.getPublishedArticles(categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
    }

    @Operation(summary = "Get a single published article by its slug (also increments its view count)")
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ArticleResponseDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(articleService.getPublishedBySlug(slug)));
    }
}
