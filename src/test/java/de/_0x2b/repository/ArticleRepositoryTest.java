package de._0x2b.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ArticleRepositoryTest {

    @Test
    void escapeIlike_leavesPlainTokenUntouched() {
        assertEquals("postgres", ArticleRepository.escapeIlike("postgres"));
    }

    @Test
    void escapeIlike_escapesPercentSoItCannotWidenThePattern() {
        assertEquals("100\\%", ArticleRepository.escapeIlike("100%"));
    }

    @Test
    void escapeIlike_escapesUnderscoreSoItCannotMatchAnySingleChar() {
        assertEquals("feed\\_url", ArticleRepository.escapeIlike("feed_url"));
    }

    @Test
    void escapeIlike_escapesBackslashBeforeWildcards() {
        // A literal backslash must survive as an escaped backslash, otherwise it
        // would consume the following character in the ILIKE pattern.
        assertEquals("a\\\\b", ArticleRepository.escapeIlike("a\\b"));
    }

    @Test
    void escapeIlike_escapesUserSuppliedEscapeSequence() {
        // Input "\%" must not degrade into an unescaped wildcard.
        assertEquals("\\\\\\%", ArticleRepository.escapeIlike("\\%"));
    }

    @Test
    void escapeIlike_handlesEmptyToken() {
        assertEquals("", ArticleRepository.escapeIlike(""));
    }
}
