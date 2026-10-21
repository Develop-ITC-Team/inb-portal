package gov.kh.moi.inb.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Generates URL-friendly slugs from arbitrary titles/names.
 *
 * <p>Note: content in this system is primarily Khmer script, which has no case distinction and
 * whose Unicode normalization does not reduce to ASCII the way Latin diacritics do. Rather than
 * mangling Khmer titles into meaningless character soup, non-Latin scripts are preserved as-is
 * (URL-encoded by the browser at request time); only whitespace/punctuation is normalized and a
 * uniqueness suffix is appended by the caller when a collision occurs.
 */
public final class SlugUtil {

    private static final Pattern WHITESPACE = Pattern.compile("[\\s]+");
    // \p{M} (combining marks) is essential for Khmer: dependent vowel signs and other
    // diacritics are category Mark, not Letter, so omitting them here silently destroys
    // Khmer text - a consonant skeleton survives, but the vowels needed to read it don't.
    private static final Pattern DISALLOWED = Pattern.compile("[^\\p{L}\\p{Nd}\\p{M}-]");
    private static final Pattern MULTI_DASH = Pattern.compile("-{2,}");

    private SlugUtil() {
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFC);
        String withDashes = WHITESPACE.matcher(normalized).replaceAll("-");
        String cleaned = DISALLOWED.matcher(withDashes).replaceAll("");
        String collapsed = MULTI_DASH.matcher(cleaned).replaceAll("-");
        String trimmedDashes = collapsed.replaceAll("^-+|-+$", "");
        return trimmedDashes.toLowerCase();
    }
}
