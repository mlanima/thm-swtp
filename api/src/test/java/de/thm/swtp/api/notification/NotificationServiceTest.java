package de.thm.swtp.api.notification;

import de.thm.swtp.api.notification.event.ProjectInviteCreatedEvent;
import de.thm.swtp.api.notification.service.NotificationService;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void setUp() throws Exception {
        ReflectionTestUtils.setField(notificationService, "mailFrom", "noreply@swtp-ss26.de");
        ReflectionTestUtils.setField(notificationService, "frontendUrl", "http://localhost:4200");
        ReflectionTestUtils.setField(notificationService, "mailLanguage", "de");
        notificationService.loadTemplates();
    }

    @Test
    void sendInviteMail_shouldSendMailWithCorrectRecipient() {
        MimeMessage mimeMessage = mock(MimeMessage.class);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        when(messageSource.getMessage(anyString(), any(), any(Locale.class))).thenReturn("test");

        ProjectInviteCreatedEvent event = new ProjectInviteCreatedEvent(buildInvite(), "invited@example.com");

        notificationService.sendInviteMail(event);

        verify(mailSender).send(mimeMessage);
    }

    @Test
    void recoverSendInviteMail_shouldNotThrow() {
        ProjectInviteCreatedEvent event = new ProjectInviteCreatedEvent(buildInvite(), "invited@example.com");
        notificationService.recoverSendInviteMail(new RuntimeException("SMTP down"), event);
        // no exception = pass
    }

    private ProjectInvite buildInvite() {
        return ProjectInvite.builder()
                .id(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .projectName("IdeaCamp")
                .invitedByUsername("alice")
                .invitedUserId(UUID.randomUUID())
                .message("Join us!")
                .status(ProjectInviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
