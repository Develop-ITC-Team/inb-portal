package gov.kh.moi.inb.controller.admin;

import gov.kh.moi.inb.dto.request.CategoryRequestDTO;
import gov.kh.moi.inb.dto.response.ApiResponse;
import gov.kh.moi.inb.dto.response.CategoryResponseDTO;
import gov.kh.moi.inb.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin - Categories", description = "JWT-protected category management for the Back Office portal")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "List all non-deleted categories (flat)")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<List<CategoryResponseDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getAllCategories()));
    }

    @Operation(summary = "Get a category by id")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EDITOR')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(categoryService.getById(id)));
    }

    @Operation(summary = "Create a new category")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> create(@Valid @RequestBody CategoryRequestDTO request,
                                                                     Authentication authentication) {
        CategoryResponseDTO created = categoryService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", created));
    }

    @Operation(summary = "Update an existing category")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<CategoryResponseDTO>> update(@PathVariable Long id,
                                                                     @Valid @RequestBody CategoryRequestDTO request,
                                                                     Authentication authentication) {
        CategoryResponseDTO updated = categoryService.update(id, request, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", updated));
    }

    @Operation(summary = "Soft-delete a category (must have no child categories)")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id, Authentication authentication) {
        categoryService.delete(id, authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}
