package gov.kh.moi.inb.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link SlugUtil}. The Khmer cases here matter more than they might look:
 * this service's content is primarily Khmer-language, and Khmer dependent vowel signs are
 * Unicode category Mark, not Letter - a naive letters-only allow-list silently strips them,
 * leaving an unreadable consonant skeleton instead of the actual word. Caught via a live smoke
 * test against real seed data, not by any of the (English-only) existing test fixtures.
 */
class SlugUtilTest {

    @Test
    void toSlug_lowercasesAndDashesLatinTitle() {
        assertThat(SlugUtil.toSlug("Welcome to the INB Portal")).isEqualTo("welcome-to-the-inb-portal");
    }

    @Test
    void toSlug_collapsesRepeatedWhitespaceAndDashes() {
        assertThat(SlugUtil.toSlug("Too    Many   Spaces")).isEqualTo("too-many-spaces");
    }

    @Test
    void toSlug_trimsLeadingAndTrailingDashes() {
        assertThat(SlugUtil.toSlug("  --Padded Title--  ")).isEqualTo("padded-title");
    }

    @Test
    void toSlug_stripsPunctuation() {
        assertThat(SlugUtil.toSlug("Breaking: News, Today!")).isEqualTo("breaking-news-today");
    }

    @Test
    void toSlug_returnsEmpty_forBlankOrNullInput() {
        assertThat(SlugUtil.toSlug("")).isEmpty();
        assertThat(SlugUtil.toSlug("   ")).isEmpty();
        assertThat(SlugUtil.toSlug(null)).isEmpty();
    }

    @Test
    void toSlug_preservesKhmerDependentVowelSigns() {
        // "Ministry of Information" - relies on dependent vowel signs (Unicode category Mark)
        // attached to the preceding consonant. If those marks are stripped, only a meaningless
        // consonant skeleton survives.
        String title = "ក្រសួងព័ត៌មាន";

        String slug = SlugUtil.toSlug(title);

        assertThat(slug).isEqualTo(title);
    }

    @Test
    void toSlug_replacesSpaceBetweenKhmerWordsWithDash() {
        String slug = SlugUtil.toSlug("ក្រសួង ព័ត៌មាន");
        assertThat(slug).isEqualTo("ក្រសួង-ព័ត៌មាន");
    }
}
