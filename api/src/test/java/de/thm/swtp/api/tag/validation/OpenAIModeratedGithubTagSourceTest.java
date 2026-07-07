package de.thm.swtp.api.tag.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpenAIModeratedGithubTagSourceTest {

    @Mock
    private OpenAIModerationClient moderationClient;

    @Mock
    private BlocklistService blocklistService;

    @Mock
    private GitHubTopicsClient gitHubTopicsClient;

    private OpenAIModeratedGithubTagSource tagSource;

    @BeforeEach
    void setUp() {
        tagSource = new OpenAIModeratedGithubTagSource(
                moderationClient, blocklistService, gitHubTopicsClient);
    }

    @Test
    void shouldAcceptTagWhenBothPass() {
        when(moderationClient.isFlagged("typescript")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("typescript")).thenReturn(true);

        assertThat(tagSource.tagExists("typescript")).isTrue();
        verify(moderationClient).isFlagged("typescript");
        verify(gitHubTopicsClient).tagExists("typescript");
        verifyNoInteractions(blocklistService);
    }

    @Test
    void shouldRejectTagWhenFlaggedByOpenAI() {
        when(moderationClient.isFlagged("fuck")).thenReturn(true);

        assertThat(tagSource.tagExists("fuck")).isFalse();
        verify(moderationClient).isFlagged("fuck");
        verifyNoInteractions(gitHubTopicsClient, blocklistService);
    }

    @Test
    void shouldRejectTagWhenNotFoundOnGitHub() {
        when(moderationClient.isFlagged("4gotjwp")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("4gotjwp")).thenReturn(false);

        assertThat(tagSource.tagExists("4gotjwp")).isFalse();
        verify(moderationClient).isFlagged("4gotjwp");
        verify(gitHubTopicsClient).tagExists("4gotjwp");
        verifyNoInteractions(blocklistService);
    }

    @Test
    void shouldFallbackToBlocklistWhenOpenAiThrows() {
        when(moderationClient.isFlagged("fuck")).thenThrow(new TagValidationException("down"));
        when(blocklistService.contains("fuck")).thenReturn(true);

        assertThat(tagSource.tagExists("fuck")).isFalse();
        verify(moderationClient).isFlagged("fuck");
        verify(blocklistService).contains("fuck");
        verifyNoInteractions(gitHubTopicsClient);
    }

    @Test
    void shouldUseGitHubWhenOpenAiFailsAndBlocklistPasses() {
        when(moderationClient.isFlagged("typescript")).thenThrow(new TagValidationException("down"));
        when(blocklistService.contains("typescript")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("typescript")).thenReturn(true);

        assertThat(tagSource.tagExists("typescript")).isTrue();
        verify(moderationClient).isFlagged("typescript");
        verify(blocklistService).contains("typescript");
        verify(gitHubTopicsClient).tagExists("typescript");
    }

    @Test
    void shouldRejectWhenOpenAiFailsAndBlocklistCatches() {
        when(moderationClient.isFlagged("fuck")).thenThrow(new TagValidationException("down"));
        when(blocklistService.contains("fuck")).thenReturn(true);

        assertThat(tagSource.tagExists("fuck")).isFalse();
        verify(moderationClient).isFlagged("fuck");
        verify(blocklistService).contains("fuck");
        verifyNoInteractions(gitHubTopicsClient);
    }

    @Test
    void shouldNormalizeInputToLowerCase() {
        when(moderationClient.isFlagged("typescript")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("typescript")).thenReturn(true);

        assertThat(tagSource.tagExists("TypeScript")).isTrue();
        verify(moderationClient).isFlagged("typescript");
        verify(gitHubTopicsClient).tagExists("typescript");
    }

    @Test
    void shouldTrimWhitespace() {
        when(moderationClient.isFlagged("typescript")).thenReturn(false);
        when(gitHubTopicsClient.tagExists("typescript")).thenReturn(true);

        assertThat(tagSource.tagExists("  TypeScript  ")).isTrue();
        verify(moderationClient).isFlagged("typescript");
        verify(gitHubTopicsClient).tagExists("typescript");
    }
}
