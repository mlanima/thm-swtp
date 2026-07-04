package de.thm.swtp.api.moderation;

import de.thm.swtp.api.moderation.exception.ContentModerationException;
import de.thm.swtp.api.moderation.exception.ContentNotValidException;
import de.thm.swtp.api.moderation.exception.ModerationApiException;
import de.thm.swtp.api.tag.validation.BlocklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentModerationServiceTest {

    @Mock
    private ModerationClient moderationClient;

    @Mock
    private BlocklistService blocklistService;

    private ContentModerationService service;

    @BeforeEach
    void setUp() {
        service = new ContentModerationService(moderationClient, blocklistService, true);
    }

    @Test
    void shouldAcceptCleanContent() {
        when(moderationClient.isFlagged("hello world")).thenReturn(false);

        assertThat(service.isContentAppropriate("hello world")).isTrue();
        verify(moderationClient).isFlagged("hello world");
        verifyNoInteractions(blocklistService);
    }

    @Test
    void shouldRejectFlaggedContent() {
        when(moderationClient.isFlagged("offensive text")).thenReturn(true);

        assertThat(service.isContentAppropriate("offensive text")).isFalse();
        verify(moderationClient).isFlagged("offensive text");
        verifyNoInteractions(blocklistService);
    }

    @Test
    void shouldFallbackToBlocklistWhenOpenAiFails() {
        when(moderationClient.isFlagged(anyString())).thenThrow(new ModerationApiException("down"));
        when(blocklistService.containsAny("bad word")).thenReturn(true);

        assertThat(service.isContentAppropriate("bad word")).isFalse();
        verify(moderationClient).isFlagged("bad word");
        verify(blocklistService).containsAny("bad word");
    }

    @Test
    void shouldAcceptWhenOpenAiFailsAndBlocklistPasses() {
        when(moderationClient.isFlagged(anyString())).thenThrow(new ModerationApiException("down"));
        when(blocklistService.containsAny("clean content")).thenReturn(false);

        assertThat(service.isContentAppropriate("clean content")).isTrue();
        verify(blocklistService).containsAny("clean content");
    }

    @Test
    void shouldThrowContentModerationExceptionWhenOpenAiFailsAndNoFallback() {
        service = new ContentModerationService(moderationClient, blocklistService, false);
        when(moderationClient.isFlagged(anyString())).thenThrow(new ModerationApiException("down"));

        assertThatThrownBy(() -> service.isContentAppropriate("any text"))
                .isInstanceOf(ContentModerationException.class)
                .hasMessage("Content moderation temporarily unavailable");
    }

    @Test
    void assertAppropriateShouldThrowWhenFlagged() {
        when(moderationClient.isFlagged("bad")).thenReturn(true);

        assertThatThrownBy(() -> service.assertAppropriate("bad", "about"))
                .isInstanceOf(ContentNotValidException.class)
                .hasMessageContaining("about");
    }

    @Test
    void assertAppropriateShouldPassWhenClean() {
        when(moderationClient.isFlagged("good")).thenReturn(false);

        service.assertAppropriate("good", "about");
    }

    @Test
    void assertAppropriateShouldSkipNullContent() {
        service.assertAppropriate(null, "about");
        verifyNoInteractions(moderationClient);
    }

    @Test
    void assertAppropriateShouldSkipBlankContent() {
        service.assertAppropriate("  ", "about");
        verifyNoInteractions(moderationClient);
    }

    @Test
    void hashShouldBeDeterministic() {
        var hash1 = service.hash("hello world");
        var hash2 = service.hash("hello world");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hashShouldDifferForDifferentContent() {
        var hash1 = service.hash("hello");
        var hash2 = service.hash("world");
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
