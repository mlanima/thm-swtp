package de.thm.swtp.api.tag.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModeratedTagSourceTest {

    @Mock
    private BlocklistService blocklistService;

    @Mock
    private GitHubTopicsClient gitHubTopicsClient;

    private ModeratedTagSource source;

    @BeforeEach
    void setUp() {
        source = new ModeratedTagSource(blocklistService, gitHubTopicsClient);
    }

    @Test
    void tagExists_shouldReturnFalse_whenBlocked() {
        when(blocklistService.contains("fuck")).thenReturn(true);

        boolean result = source.tagExists("fuck");

        assertThat(result).isFalse();
        verifyNoInteractions(gitHubTopicsClient);
    }

    @Test
    void tagExists_shouldDelegateToGitHub_whenNotBlocked() {
        when(blocklistService.contains("spring")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("spring")).thenReturn(true);

        boolean result = source.tagExists("spring");

        assertThat(result).isTrue();
        verify(gitHubTopicsClient).tagExists("spring");
    }

    @Test
    void tagExists_shouldTrimAndLowercaseBeforeAllChecks() {
        when(blocklistService.contains("spring")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("spring")).thenReturn(false);

        boolean result = source.tagExists("  Spring  ");

        assertThat(result).isFalse();
        verify(gitHubTopicsClient).tagExists("spring");
    }

    @Test
    void tagExists_shouldReturnFalse_whenNotBlockedAndNotOnGitHub() {
        when(blocklistService.contains("nonexistent")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("nonexistent")).thenReturn(false);

        boolean result = source.tagExists("nonexistent");

        assertThat(result).isFalse();
    }

    @Test
    void tagExists_shouldNotCallGitHub_whenBlocked() {
        when(blocklistService.contains("ass")).thenReturn(true);

        source.tagExists("ass");

        verifyNoInteractions(gitHubTopicsClient);
    }
}
