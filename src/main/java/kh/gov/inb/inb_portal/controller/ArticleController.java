package kh.gov.inb.inb_portal.controller;

import kh.gov.inb.inb_portal.entity.Article;
import kh.gov.inb.inb_portal.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/articles")
public class ArticleController
{

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService){
        this.articleService = articleService;
    }

    //get request: fetch all articles
    @GetMapping
    public List<Article> getAllArticles(){
        return articleService.getAllArticles();
    }

    //post request: create new article
    @PostMapping
    public ResponseEntity<Article> createArticle(@RequestBody Article article){
        Article savedArticle = articleService.createArticle(article);
        return ResponseEntity.ok().body(savedArticle);
    }
}
