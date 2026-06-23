package de.thm.swtp.api.notification.service;

import de.thm.swtp.api.notification.event.ProjectInviteCreatedEvent;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Value("${app.mail.language:de}")
    private String mailLanguage;

    // 3 Retries bei jedem Fehler: nach ~10s, ~60s, ~5min — danach @Recover (warn + swallow)
    @Retryable(retryFor = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 10_000, multiplier = 6, maxDelay = 300_000))
    public void sendInviteMail(ProjectInviteCreatedEvent event) throws Exception {
        ProjectInvite invite = event.invite();
        Locale locale = Locale.forLanguageTag(mailLanguage);

        String subject = messageSource.getMessage("mail.invite.subject",
                new Object[]{invite.getProjectName()}, locale);
        String greeting = messageSource.getMessage("mail.invite.greeting", null, locale);
        String body = messageSource.getMessage("mail.invite.body",
                new Object[]{invite.getInvitedByUsername(), invite.getProjectName()}, locale);
        String cta = messageSource.getMessage("mail.invite.cta", null, locale);
        String hint = messageSource.getMessage("mail.invite.hint", null, locale);
        String footer = messageSource.getMessage("mail.invite.footer", null, locale);

        String ctaUrl = frontendUrl + "/invitations";

        String template = new ClassPathResource("templates/emails/invite.html")
                .getContentAsString(StandardCharsets.UTF_8);

        String html = template
                .replace("{greeting}", greeting)
                .replace("{body}", body)
                .replace("{message}", invite.getMessage() != null ? invite.getMessage() : "")
                .replace("{cta}", cta)
                .replace("{ctaUrl}", ctaUrl)
                .replace("{hint}", hint)
                .replace("{footer}", footer);

        MimeMessage mail = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mail, false, "UTF-8");
        helper.setFrom(mailFrom);
        helper.setTo(event.invitedUserEmail());
        helper.setSubject(subject);
        helper.setText(html, true);

        mailSender.send(mail);
        log.info("Invite mail sent to {} for project '{}'", event.invitedUserEmail(), invite.getProjectName());
    }

    @Recover
    public void recoverSendInviteMail(Exception ex, ProjectInviteCreatedEvent event) {
        log.warn("Failed to send invite mail to {} after retries — giving up. Cause: {}",
                event.invitedUserEmail(), ex.getMessage());
    }
}
