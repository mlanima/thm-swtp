package de.thm.swtp.api.discord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.client.RestTemplate;

/**
 * Spring configuration that provides beans used by the Discord sync infrastructure.
 */
@Configuration
public class DiscordStreamConfig {

    /**
     * Dedicated thread pool for scheduled Discord sync tasks.
     * Uses daemon threads so they don't block JVM shutdown.
     */
    @Bean
    public ThreadPoolTaskScheduler discordSyncTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("discord-sync-");
        scheduler.setDaemon(true);
        return scheduler;
    }

    /**
     * Plain RestTemplate used for outbound HTTP calls to Discord's API.
     */
    @Bean
    public RestTemplate discordRestTemplate() {
        return new RestTemplate();
    }
}
