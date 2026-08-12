package gov.kh.moi.inb.mapper;

import gov.kh.moi.inb.dto.response.ArticleResponseDTO;
import gov.kh.moi.inb.dto.response.ArticleSummaryDTO;
import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleMedia;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = {CategoryMapper.class, ArticleMediaMapper.class})
public interface ArticleMapper {

    @Mapping(target = "category", source = "category")
    @Mapping(target = "media", source = "mediaList")
    ArticleResponseDTO toDto(Article article);

    List<ArticleResponseDTO> toDtoList(List<Article> articles);

    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "categorySlug", source = "category.slug")
    @Mapping(target = "coverImageUrl", expression = "java(resolveCoverImageUrl(article.getMediaList()))")
    @Mapping(target = "mediaCount", expression = "java(article.getMediaList() == null ? 0 : article.getMediaList().size())")
    ArticleSummaryDTO toSummaryDto(Article article);

    List<ArticleSummaryDTO> toSummaryDtoList(List<Article> articles);

    default String resolveCoverImageUrl(List<ArticleMedia> mediaList) {
        if (mediaList == null || mediaList.isEmpty()) {
            return null;
        }
        Optional<ArticleMedia> cover = mediaList.stream().filter(ArticleMedia::isCover).findFirst();
        return cover.map(ArticleMedia::getFileUrl)
                .orElseGet(() -> mediaList.stream()
                        .min(Comparator.comparing(ArticleMedia::getDisplayOrder))
                        .map(ArticleMedia::getFileUrl)
                        .orElse(null));
    }
}
