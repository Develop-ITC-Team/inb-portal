package kh.gov.inb.inb_portal.repository;

import kh.gov.inb.inb_portal.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {



}
