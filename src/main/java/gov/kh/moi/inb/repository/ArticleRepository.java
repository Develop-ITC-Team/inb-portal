package gov.kh.moi.inb.repository;

import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long>, JpaSpecificationExecutor<Article> {

    Optional<Article> findBySlugAndDeletedFalse(String slug);

    Optional<Article> findBySlugAndStatusAndDeletedFalse(String slug, ArticleStatus status);

    boolean existsBySlugAndDeletedFalse(String slug);

    boolean existsBySlugAndIdNotAndDeletedFalse(String slug, Long id);

    Page<Article> findByStatusAndDeletedFalse(ArticleStatus status, Pageable pageable);

    Page<Article> findByStatusAndCategoryIdAndDeletedFalse(ArticleStatus status, Long categoryId, Pageable pageable);

    Page<Article> findByDeletedFalse(Pageable pageable);

    @Modifying
    @Query("UPDATE Article a SET a.viewCount = a.viewCount + 1 WHERE a.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
