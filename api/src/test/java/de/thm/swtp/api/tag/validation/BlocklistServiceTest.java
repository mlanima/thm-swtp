package de.thm.swtp.api.tag.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

class BlocklistServiceTest {

    private BlocklistService service;

    @BeforeEach
    void setUp() {
        service = new BlocklistService(new PathMatchingResourcePatternResolver());
        service.init();
    }

    @Test
    void contains_shouldReturnTrue_forExactBlockedWord() {
        assertThat(service.contains("fuck")).isTrue();
    }

    @Test
    void contains_shouldReturnTrue_forMixedCaseBlockedWord() {
        assertThat(service.contains("Fuck")).isTrue();
        assertThat(service.contains("FUCK")).isTrue();
    }

    @Test
    void contains_shouldReturnFalse_forCleanWord() {
        assertThat(service.contains("typescript")).isFalse();
        assertThat(service.contains("spring")).isFalse();
        assertThat(service.contains("angular")).isFalse();
    }

    @Test
    void contains_shouldReturnFalse_forEmptyString() {
        assertThat(service.contains("")).isFalse();
    }

    @Test
    void contains_shouldTrimInput() {
        assertThat(service.contains("  fuck  ")).isTrue();
    }

    @Test
    void contains_shouldReturnFalse_forSubstringOfBlockedWord() {
        assertThat(service.contains("grass")).isFalse();
    }

    @Test
    void contains_shouldReturnFalse_forHyphenatedTagsContainingBlockedWord() {
        assertThat(service.contains("class-end")).isFalse();
        assertThat(service.contains("ass-end")).isFalse();
    }
}
