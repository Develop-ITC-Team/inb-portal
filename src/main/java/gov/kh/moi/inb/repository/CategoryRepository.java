package gov.kh.moi.inb.repository;

import gov.kh.moi.inb.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndIdNotAndDeletedFalse(String slug, Long id);

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL AND c.deleted = false ORDER BY c.displayOrder ASC")
    List<Category> findRootCategories();

    List<Category> findByParentIdAndDeletedFalseOrderByDisplayOrderAsc(Long parentId);

    List<Category> findAllByDeletedFalseOrderByDisplayOrderAsc();
}
