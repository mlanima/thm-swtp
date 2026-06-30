package de.thm.swtp.api.tag.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagValidationServiceTest {

    @Mock
    private TagSource tagSource;

    private TagValidationService service;

    @BeforeEach
    void setUp() {
        service = new TagValidationService(tagSource);
    }

    @Test
    void isValidTag_shouldDelegateToTagSource() {
        when(tagSource.tagExists("spring")).thenReturn(true);

        boolean result = service.isValidTag("spring");

        assertThat(result).isTrue();
    }

    @Test
    void isValidTag_shouldReturnFalse_whenTagSourceReturnsFalse() {
        when(tagSource.tagExists("nonexistent")).thenReturn(false);

        boolean result = service.isValidTag("nonexistent");

        assertThat(result).isFalse();
    }

    @Test
    void isValidTag_shouldIgnoreCaseInKey() {
        when(tagSource.tagExists("spring")).thenReturn(true);

        boolean resultUpper = service.isValidTag("Spring");
        boolean resultMixed = service.isValidTag("SPRING");

        assertThat(resultUpper).isTrue();
        assertThat(resultMixed).isTrue();
    }
}
