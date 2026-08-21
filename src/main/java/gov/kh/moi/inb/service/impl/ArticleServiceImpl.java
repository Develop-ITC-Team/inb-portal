package gov.kh.moi.inb.service.impl;

import gov.kh.moi.inb.dto.request.ArticleMediaRequestDTO;
import gov.kh.moi.inb.dto.request.ArticleRequestDTO;
import gov.kh.moi.inb.dto.response.ArticleResponseDTO;
import gov.kh.moi.inb.dto.response.ArticleSummaryDTO;
import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleMedia;
import gov.kh.moi.inb.entity.ArticleStatus;
import gov.kh.moi.inb.entity.Category;
import gov.kh.moi.inb.exception.ResourceNotFoundException;
import gov.kh.moi.inb.mapper.ArticleMapper;
import gov.kh.moi.inb.repository.ArticleRepository;
import gov.kh.moi.inb.repository.CategoryRepository;
import gov.kh.moi.inb.repository.spec.ArticleSpecifications;
import gov.kh.moi.inb.service.ArticleService;
import gov.kh.moi.inb.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;
    private final CategoryRepository categoryRepository;
    private final ArticleMapper articleMapper;

    // =========================================================================================
    // Public read operations - PUBLISHED articles only, never expose DRAFT/ARCHIVED content
    // or internal audit fields to citizens.
    // =========================================================================================

    @Override
    public Page<ArticleSummaryDTO> getPublishedArticles(Long categoryId, Pageable pageable) {
        Page<Article> page = (categoryId != null)
                ? articleRepository.findByStatusAndCategoryIdAndDeletedFalse(ArticleStatus.PUBLISHED, categoryId, pageable)
                : articleRepository.findByStatusAndDeletedFalse(ArticleStatus.PUBLISHED, pageable);
        return page.map(articleMapper::toSummaryDto);
    }

    @Override
    @Transactional // upgrades to a write transaction because of the view-count increment below
    public ArticleResponseDTO getPublishedBySlug(String slug) {
        Article article = articleRepository.findBySlugAndStatusAndDeletedFalse(slug, ArticleStatus.PUBLISHED)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", slug));
        articleRepository.incrementViewCount(article.getId());
        return articleMapper.toDto(article);
    }

    // =========================================================================================
    // Admin operations - any status, protected by JWT + RBAC at the controller/security layer.
    // =========================================================================================

    @Override
    public Page<ArticleSummaryDTO> searchArticles(ArticleStatus status, Long categoryId, String search,
                                                   Boolean featured, Pageable pageable) {
        Page<Article> page = articleRepository.findAll(
                ArticleSpecifications.withFilters(status, categoryId, search, featured), pageable);
        return page.map(articleMapper::toSummaryDto);
    }

    @Override
    public ArticleResponseDTO getById(Long id) {
        return articleMapper.toDto(findActiveById(id));
    }

    @Override
    @Transactional
    public ArticleResponseDTO create(ArticleRequestDTO request, String actor) {
        Category category = findActiveCategory(request.getCategoryId());
        String slug = resolveSlug(request.getSlug(), request.getTitle(), null);

        Article article = Article.builder()
                .category(category)
                .title(request.getTitle())
                .slug(slug)
                .excerpt(request.getExcerpt())
                .content(request.getContent())
                .authorName(request.getAuthorName())
                .status(ArticleStatus.DRAFT)
                .featured(request.getFeatured() != null && request.getFeatured())
                .build();

        applyMedia(article, request.getMedia());

        Article saved = articleRepository.save(article);
        log.info("Article created: id={}, slug={}, by={}", saved.getId(), saved.getSlug(), actor);
        return articleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ArticleResponseDTO update(Long id, ArticleRequestDTO request, String actor) {
        Article article = findActiveById(id);

        if (request.getCategoryId() != null) {
            article.setCategory(findActiveCategory(request.getCategoryId()));
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            article.setTitle(request.getTitle());
        }
        String slug = resolveSlug(request.getSlug(), article.getTitle(), id);
        article.setSlug(slug);
        article.setExcerpt(request.getExcerpt());
        if (request.getContent() != null && !request.getContent().isBlank()) {
            article.setContent(request.getContent());
        }
        article.setAuthorName(request.getAuthorName());
        if (request.getFeatured() != null) {
            article.setFeatured(request.getFeatured());
        }

        if (request.getMedia() != null) {
            article.getMediaList().clear();
            applyMedia(article, request.getMedia());
        }

        Article saved = articleRepository.save(article);
        log.info("Article updated: id={}, by={}", saved.getId(), actor);
        return articleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ArticleResponseDTO changeStatus(Long id, ArticleStatus newStatus, String actor) {
        Article article = findActiveById(id);
        ArticleStatus previous = article.getStatus();
        article.setStatus(newStatus);

        if (newStatus == ArticleStatus.PUBLISHED && article.getPublishedAt() == null) {
            article.setPublishedAt(OffsetDateTime.now());
        }

        Article saved = articleRepository.save(article);
        log.info("Article status changed: id={}, {} -> {}, by={}", id, previous, newStatus, actor);
        return articleMapper.toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id, String actor) {
        Article article = findActiveById(id);
        article.setDeleted(true);
        articleRepository.save(article);
        log.info("Article soft-deleted: id={}, by={}", id, actor);
    }

    // =========================================================================================
    // Helpers
    // =========================================================================================

    private void applyMedia(Article article, List<ArticleMediaRequestDTO> mediaRequests) {
        if (mediaRequests == null) {
            return;
        }
        for (int i = 0; i < mediaRequests.size(); i++) {
            ArticleMediaRequestDTO m = mediaRequests.get(i);
            ArticleMedia media = ArticleMedia.builder()
                    .fileUrl(m.getFileUrl())
                    .mediaType(m.getMediaType() != null ? m.getMediaType() : gov.kh.moi.inb.entity.MediaType.IMAGE)
                    .caption(m.getCaption())
                    .displayOrder(m.getDisplayOrder() != null ? m.getDisplayOrder() : i)
                    .cover(Boolean.TRUE.equals(m.getCover()))
                    .build();
            article.addMedia(media);
        }
    }

    private Article findActiveById(Long id) {
        Article article = articleRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Article", id));
        if (article.isDeleted()) {
            throw ResourceNotFoundException.of("Article", id);
        }
        return article;
    }

    private Category findActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", categoryId));
        if (category.isDeleted()) {
            throw ResourceNotFoundException.of("Category", categoryId);
        }
        return category;
    }

    private String resolveSlug(String providedSlug, String fallbackSource, Long excludeId) {
        String base = (providedSlug != null && !providedSlug.isBlank())
                ? SlugUtil.toSlug(providedSlug)
                : SlugUtil.toSlug(fallbackSource);

        if (base.isBlank()) {
            throw new IllegalArgumentException("Unable to derive a valid slug from the provided title");
        }

        String candidate = base;
        int suffix = 1;
        while (excludeId == null
                ? articleRepository.existsBySlugAndDeletedFalse(candidate)
                : articleRepository.existsBySlugAndIdNotAndDeletedFalse(candidate, excludeId)) {
            suffix++;
            candidate = base + "-" + suffix;
        }
        return candidate;
    }
}
