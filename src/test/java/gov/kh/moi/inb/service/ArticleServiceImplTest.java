package gov.kh.moi.inb.service;

import gov.kh.moi.inb.dto.request.ArticleRequestDTO;
import gov.kh.moi.inb.dto.response.ArticleResponseDTO;
import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleStatus;
import gov.kh.moi.inb.entity.Category;
import gov.kh.moi.inb.exception.ResourceNotFoundException;
import gov.kh.moi.inb.mapper.ArticleMapper;
import gov.kh.moi.inb.repository.ArticleRepository;
import gov.kh.moi.inb.repository.CategoryRepository;
import gov.kh.moi.inb.service.impl.ArticleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the core article business rules: slug generation/uniqueness, publish workflow,
 * and not-found handling for soft-deleted records. Repository and mapper collaborators are
 * mocked so these tests run fast and exercise only the service layer's own logic.
 */
@ExtendWith(MockitoExtension.class)
class ArticleServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private ArticleServiceImpl articleService;

    private Category sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = Category.builder().id(1L).name("National News").slug("national-news").active(true).build();
    }

    @Test
    void create_generatesSlugFromTitle_whenSlugNotProvided() {
        ArticleRequestDTO request = new ArticleRequestDTO();
        request.setCategoryId(1L);
        request.setTitle("Welcome to the INB Portal");
        request.setContent("Official content goes here.");
        request.setAuthorName("System Admin");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(articleRepository.existsBySlugAndDeletedFalse("welcome-to-the-inb-portal")).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
        when(articleMapper.toDto(any(Article.class))).thenReturn(ArticleResponseDTO.builder().build());

        articleService.create(request, "editor1");

        var captor = org.mockito.ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("welcome-to-the-inb-portal");
        assertThat(captor.getValue().getStatus()).isEqualTo(ArticleStatus.DRAFT);
    }

    @Test
    void create_appendsSuffix_whenSlugAlreadyExists() {
        ArticleRequestDTO request = new ArticleRequestDTO();
        request.setCategoryId(1L);
        request.setTitle("Welcome to the INB Portal");
        request.setContent("Official content goes here.");

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(articleRepository.existsBySlugAndDeletedFalse("welcome-to-the-inb-portal")).thenReturn(true);
        when(articleRepository.existsBySlugAndDeletedFalse("welcome-to-the-inb-portal-2")).thenReturn(false);
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
        when(articleMapper.toDto(any(Article.class))).thenReturn(ArticleResponseDTO.builder().build());

        articleService.create(request, "editor1");

        var captor = org.mockito.ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(captor.capture());
        assertThat(captor.getValue().getSlug()).isEqualTo("welcome-to-the-inb-portal-2");
    }

    @Test
    void changeStatus_setsPublishedAt_whenTransitioningToPublished() {
        Article article = Article.builder().id(5L).category(sampleCategory).title("t").slug("t")
                .content("c").status(ArticleStatus.DRAFT).build();

        when(articleRepository.findById(5L)).thenReturn(Optional.of(article));
        when(articleRepository.save(any(Article.class))).thenAnswer(inv -> inv.getArgument(0));
        when(articleMapper.toDto(any(Article.class))).thenReturn(ArticleResponseDTO.builder().build());

        articleService.changeStatus(5L, ArticleStatus.PUBLISHED, "editor1");

        var captor = org.mockito.ArgumentCaptor.forClass(Article.class);
        verify(articleRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ArticleStatus.PUBLISHED);
        assertThat(captor.getValue().getPublishedAt()).isNotNull();
    }

    @Test
    void getById_throwsNotFound_whenArticleIsSoftDeleted() {
        Article deleted = Article.builder().id(9L).category(sampleCategory).title("t").slug("t")
                .content("c").deleted(true).build();
        when(articleRepository.findById(9L)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> articleService.getById(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getPublishedBySlug_incrementsViewCount() {
        Article article = Article.builder().id(3L).category(sampleCategory).title("t").slug("published-article")
                .content("c").status(ArticleStatus.PUBLISHED).build();
        when(articleRepository.findBySlugAndStatusAndDeletedFalse("published-article", ArticleStatus.PUBLISHED))
                .thenReturn(Optional.of(article));
        when(articleMapper.toDto(article)).thenReturn(ArticleResponseDTO.builder().build());

        articleService.getPublishedBySlug("published-article");

        verify(articleRepository).incrementViewCount(3L);
    }
}
