package gov.kh.moi.inb.service;

import gov.kh.moi.inb.dto.request.ArticleRequestDTO;
import gov.kh.moi.inb.dto.response.ArticleResponseDTO;
import gov.kh.moi.inb.dto.response.ArticleSummaryDTO;
import gov.kh.moi.inb.entity.ArticleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ArticleService {

    // ---- Public read operations (PUBLISHED only) ----
    Page<ArticleSummaryDTO> getPublishedArticles(Long categoryId, Pageable pageable);

    ArticleResponseDTO getPublishedBySlug(String slug);

    // ---- Admin operations (any status) ----
    Page<ArticleSummaryDTO> searchArticles(ArticleStatus status, Long categoryId, String search,
                                            Boolean featured, Pageable pageable);

    ArticleResponseDTO getById(Long id);

    ArticleResponseDTO create(ArticleRequestDTO request, String actor);

    ArticleResponseDTO update(Long id, ArticleRequestDTO request, String actor);

    ArticleResponseDTO changeStatus(Long id, ArticleStatus newStatus, String actor);

    void delete(Long id, String actor);
}
