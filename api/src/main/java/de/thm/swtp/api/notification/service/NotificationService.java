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
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.web.util.HtmlUtils;

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

    private String inviteTemplate;

    @PostConstruct
    public void loadTemplates() throws Exception {
        inviteTemplate = new ClassPathResource("templates/emails/invite.html")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    // 3 Retries bei jedem Fehler: nach ~10s, ~60s, ~5min — danach @Recover (warn + swallow)
    @Retryable(retryFor = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 10_000, multiplier = 6, maxDelay = 300_000))
    public void sendInviteMail(ProjectInviteCreatedEvent event) throws Exception {
        ProjectInvite invite = event.invite();
        Locale locale = Locale.forLanguageTag(mailLanguage);

        String safeProjectName  = HtmlUtils.htmlEscape(invite.getProjectName());
        String safeInviterName  = HtmlUtils.htmlEscape(invite.getInvitedByUsername());

        String subject = messageSource.getMessage("mail.invite.subject",
                new Object[]{safeProjectName}, locale);
        String greeting = messageSource.getMessage("mail.invite.greeting", null, locale);
        String body = messageSource.getMessage("mail.invite.body",
                new Object[]{safeInviterName, safeProjectName}, locale);
        String cta = messageSource.getMessage("mail.invite.cta", null, locale);
        String hint = messageSource.getMessage("mail.invite.hint", null, locale);
        String footer = messageSource.getMessage("mail.invite.footer", null, locale);

        String ctaUrl = frontendUrl + "/my-projects";

        String messageBlock = (invite.getMessage() != null && !invite.getMessage().isBlank())
                ? "<div class=\"message-box\">" + HtmlUtils.htmlEscape(invite.getMessage()) + "</div>"
                : "";

        String html = inviteTemplate
                .replace("{lang}", mailLanguage)
                .replace("{greeting}", greeting)
                .replace("{body}", body)
                .replace("{message-block}", messageBlock)
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
