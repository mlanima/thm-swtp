package de.thm.swtp.api.notification.service;

import de.thm.swtp.api.notification.event.ProfessorRequestVerificationCreatedEvent;
import de.thm.swtp.api.notification.event.ProjectInviteCreatedEvent;
import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import jakarta.annotation.PostConstruct;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
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

    @Value("${app.backend-url}")
    private String backendUrl;

    @Value("${app.mail.language:de}")
    private String mailLanguage;

    private String inviteTemplate;
    private String professorRequestVerificationTemplate;


    @PostConstruct
    public void loadTemplates() throws Exception {
        inviteTemplate = new ClassPathResource("templates/emails/invite.html")
                .getContentAsString(StandardCharsets.UTF_8);

        professorRequestVerificationTemplate = new ClassPathResource("templates/emails/professor-request-verification.html")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    // 3 Retries bei jedem Fehler: nach ~10s, ~60s, ~5min — danach @Recover (warn + swallow)
    @Retryable(retryFor = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 10_000, multiplier = 6, maxDelay = 300_000))
    public void sendInviteMail(ProjectInviteCreatedEvent event) {
        ProjectInvite invite = event.invite();
        Locale locale = Locale.forLanguageTag(mailLanguage);

        String safeProjectName  = HtmlUtils.htmlEscape(invite.getProjectName());
        String safeInviterName  = HtmlUtils.htmlEscape(invite.getInvitedByUsername());

        String subject = messageSource.getMessage("mail.invite.subject",
                new Object[]{invite.getProjectName()}, locale);
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

        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(event.invitedUserEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mail);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Failed to build invite mail for " + event.invitedUserEmail(), e);
        }
        log.info("Invite mail sent: invite={}, to={}, project='{}', locale={}",
                invite.getId(), event.invitedUserEmail(), invite.getProjectName(), locale);
    }

    @Retryable(retryFor = Exception.class, maxAttempts = 4, backoff = @Backoff(delay = 10_000, multiplier = 6, maxDelay = 300_000))
    public void sendProfessorRequestVerificationMail(ProfessorRequestVerificationCreatedEvent event) {
        ProfessorRequest request = event.professorRequest();
        Locale locale = Locale.forLanguageTag(mailLanguage);

        String verificationUrl = frontendUrl + "/settings?tab=professor-request&verifyToken=" + event.verificationToken();

        String safeUsername = HtmlUtils.htmlEscape(request.getRequestingUsername());
        String safeVerificationUrl = HtmlUtils.htmlEscape(verificationUrl);

        String subject = messageSource.getMessage("mail.professor.verification.subject", null, locale);
        String greeting = messageSource.getMessage("mail.professor.verification.greeting",
                new Object[]{safeUsername}, locale);
        String body = messageSource.getMessage("mail.professor.verification.body", null, locale);
        String cta = messageSource.getMessage("mail.professor.verification.cta", null, locale);
        String hint = messageSource.getMessage("mail.professor.verification.hint", null, locale);
        String footer = messageSource.getMessage("mail.professor.verification.footer", null, locale);

        String html = professorRequestVerificationTemplate
                .replace("{lang}", mailLanguage)
                .replace("{greeting}", greeting)
                .replace("{body}", body)
                .replace("{verificationUrl}", safeVerificationUrl)
                .replace("{cta}", cta)
                .replace("{hint}", hint)
                .replace("{footer}", footer);

        try {
            MimeMessage mail = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mail, false, "UTF-8");
            helper.setFrom(mailFrom);
            helper.setTo(request.getEmail());
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mail);
        } catch (jakarta.mail.MessagingException e) {
            throw new RuntimeException("Failed to build professor request verification mail for " + request.getEmail(), e);
        }

        log.info("Professor request verification mail sent: request={}, to={}, user={}, locale={}",
                request.getId(), request.getEmail(), request.getRequestingUsername(), locale);
    }

    @Recover
    public void recoverSendInviteMail(Exception ex, ProjectInviteCreatedEvent event) {
        log.error("Invite mail failed after retries: invite={}, to={}, cause={}",
                event.invite().getId(), event.invitedUserEmail(), ex.getMessage());
    }

    @Recover
    public void recoverSendProfessorRequestVerificationMail(Exception ex, ProfessorRequestVerificationCreatedEvent event) {
        ProfessorRequest request = event.professorRequest();
        log.error("Professor request verification mail failed after retries: request={}, to={}, user={}, cause={}",
                request.getId(), request.getEmail(), request.getRequestingUsername(), ex.getMessage());
    }
}
