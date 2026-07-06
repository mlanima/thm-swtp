package de.thm.swtp.api.discord.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
public class DiscordStreamConfig {

    @Bean
    public ThreadPoolTaskScheduler discordSyncTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);
        scheduler.setThreadNamePrefix("discord-sync-");
        scheduler.setDaemon(true);
        return scheduler;
    }
}
