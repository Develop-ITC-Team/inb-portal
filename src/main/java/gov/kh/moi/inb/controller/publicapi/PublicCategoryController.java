package gov.kh.moi.inb.controller.publicapi;

import gov.kh.moi.inb.dto.response.ApiResponse;
import gov.kh.moi.inb.dto.response.CategoryResponseDTO;
import gov.kh.moi.inb.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Public - Categories", description = "Unauthenticated read endpoints consumed by the citizen-facing portal")
@RestController
@RequestMapping("/api/public/categories")
@RequiredArgsConstructor
public class PublicCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "Get the active category tree (for the site's navigation menu)")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getCategoryTree() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getPublicCategoryTree()));
    }

    @Operation(summary = "Get a single category by its slug")
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getBySlug(slug)));
    }
}
