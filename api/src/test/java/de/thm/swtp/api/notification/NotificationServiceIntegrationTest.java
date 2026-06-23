package de.thm.swtp.api.notification;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.GreenMailUtil;
import com.icegreen.greenmail.util.ServerSetupTest;
import de.thm.swtp.api.notification.event.ProjectInviteCreatedEvent;
import de.thm.swtp.api.notification.service.NotificationService;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.domain.ProjectInviteStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.mail.host=localhost",
        "spring.mail.port=3025",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false",
        "app.mail.from=noreply@swtp-ss26.de",
        "app.mail.language=de",
        "app.frontend-url=http://localhost:4200",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://dummy"
})
class NotificationServiceIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private NotificationService notificationService;

    @Test
    void sendInviteMail_shouldDeliverMailWithSubjectAndRecipient() throws Exception {
        ProjectInvite invite = ProjectInvite.builder()
                .id(UUID.randomUUID())
                .projectId(UUID.randomUUID())
                .projectName("IdeaCamp")
                .invitedByUsername("alice")
                .invitedUserId(UUID.randomUUID())
                .message("Komm ins Team!")
                .status(ProjectInviteStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        notificationService.sendInviteMail(new ProjectInviteCreatedEvent(invite, "bob@example.com"));

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getAllRecipients()[0].toString()).isEqualTo("bob@example.com");
        assertThat(received[0].getSubject()).contains("IdeaCamp");

        String body = GreenMailUtil.getBody(received[0]);
        assertThat(body).contains("alice");
        assertThat(body).contains("IdeaCamp");
    }
}
