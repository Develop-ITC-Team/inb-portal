package kh.gov.inb.inb_portal.service;

import kh.gov.inb.inb_portal.entity.Article;
import kh.gov.inb.inb_portal.repository.ArticleRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {
    public final ArticleRepository articleRepository;

    //
    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;

    }
    //get all article
    public List<Article> getAllArticles() {
        return articleRepository.findAll();
    }

    //save article
    public Article createArticle (Article article) {
        if (article.getStatus() == null){
            article.setStatus("DRAFT");
        }
        return articleRepository.save(article);
    }
}
