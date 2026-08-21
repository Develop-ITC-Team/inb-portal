package gov.kh.moi.inb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Entry point for the INB Public Portal backend microservice.
 *
 * <p>Serves both:
 * <ul>
 *   <li>Public, unauthenticated read endpoints ({@code /api/public/**}) consumed by the
 *       citizen-facing portal, and</li>
 *   <li>JWT-protected administrative write endpoints ({@code /api/admin/**}) consumed by the
 *       Back Office portal.</li>
 * </ul>
 * The two are intentionally kept in separate controller packages with independent security
 * rules so this module can be split into two standalone microservices later with no changes
 * to the domain/service layer.
 */
@SpringBootApplication
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class InbPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(InbPortalApplication.class, args);
    }
}
