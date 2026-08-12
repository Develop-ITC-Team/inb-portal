package gov.kh.moi.inb.repository;

import gov.kh.moi.inb.entity.ArticleMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleMediaRepository extends JpaRepository<ArticleMedia, Long> {
    List<ArticleMedia> findByArticleIdOrderByDisplayOrderAsc(Long articleId);
}
