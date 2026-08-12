package gov.kh.moi.inb.mapper;

import gov.kh.moi.inb.dto.response.ArticleMediaDTO;
import gov.kh.moi.inb.entity.ArticleMedia;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ArticleMediaMapper {
    ArticleMediaDTO toDto(ArticleMedia media);
    List<ArticleMediaDTO> toDtoList(List<ArticleMedia> mediaList);
}
