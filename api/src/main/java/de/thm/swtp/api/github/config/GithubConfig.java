package de.thm.swtp.api.github.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GithubOAuthProperties.class)
public class GithubConfig {
}
