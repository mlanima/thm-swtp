package de.thm.swtp.api.professorRequest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.professor-request")
public record ProfessorRequestProperties(List<String> allowedEmailDomains, int verificationTokenValidHours) {
}
