package gov.kh.moi.inb.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Standard audit columns shared by every entity in this service: created/updated timestamps
 * and actors, plus a soft-delete flag. Populated automatically by Spring Data JPA auditing
 * (see {@code AuditorAware} bean) rather than being set manually in service code, which keeps
 * audit trails consistent and tamper-resistant across the codebase.
 *
 * <p>Uses {@code @SuperBuilder} (not plain {@code @Builder}) so that subclasses' generated
 * builders include these inherited fields too - a plain {@code @Builder} on a subclass silently
 * ignores fields declared in a mapped superclass. {@code @NoArgsConstructor} must be declared
 * explicitly alongside it: {@code @SuperBuilder} alone replaces the implicit default
 * constructor with a builder-only one, which breaks subclasses' JPA-required no-arg
 * constructors (they call {@code super()}, which would otherwise no longer exist).
 *
 * <p>{@code createdAt}/{@code updatedAt} use {@link Instant} rather than
 * {@link java.time.OffsetDateTime}: Spring Data JPA's built-in auditing date converter does
 * not support {@code OffsetDateTime} as a target field type (it throws
 * {@code InvalidDataAccessApiUsageException} at persist time), while {@code Instant} is fully
 * supported and is arguably the more correct choice for an audit timestamp anyway - an
 * unambiguous point in time, with no timezone-offset bookkeeping. Still maps to a
 * {@code TIMESTAMPTZ} column in PostgreSQL with no schema changes needed.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseAuditEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false, length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Soft-delete flag. Records are never physically removed from the database so that
     * historical/audit integrity is preserved; repositories filter this out by default
     * via {@code @Where} or explicit query clauses.
     */
    @Builder.Default
    @Column(name = "deleted", nullable = false)
    private boolean deleted = false;
}