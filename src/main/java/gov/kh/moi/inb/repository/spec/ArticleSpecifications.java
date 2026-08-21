package gov.kh.moi.inb.repository.spec;

import gov.kh.moi.inb.entity.Article;
import gov.kh.moi.inb.entity.ArticleStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Composable JPA Specifications for admin article search/filtering, avoiding a combinatorial
 * explosion of repository query methods as filter criteria grow (status + category + search +
 * featured + date range, etc.).
 */
public final class ArticleSpecifications {

    private ArticleSpecifications() {
    }

    public static Specification<Article> withFilters(ArticleStatus status,
                                                       Long categoryId,
                                                       String searchTerm,
                                                       Boolean featured) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.<Boolean>get("deleted")));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (featured != null) {
                predicates.add(cb.equal(root.get("featured"), featured));
            }
            if (searchTerm != null && !searchTerm.isBlank()) {
                String pattern = "%" + searchTerm.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.<String>get("title")), pattern),
                        cb.like(cb.lower(root.<String>get("excerpt")), pattern)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
