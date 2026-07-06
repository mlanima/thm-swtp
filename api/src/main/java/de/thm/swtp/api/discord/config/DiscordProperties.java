package de.thm.swtp.api.discord.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "discord")
@Getter
@Setter
public class DiscordProperties {

    private Streams streams = new Streams();
    private Bot bot = new Bot();

    @Getter
    @Setter
    public static class Streams {
        private String outbound = "stream:discord:sync";
        private String inbound = "stream:platform:sync";
        private String consumerGroup = "spring-platform";
        private int batchSize = 10;
        private int blockMs = 5000;
        private int pendingTimeoutMs = 30000;
    }

    @Getter
    @Setter
    public static class Bot {
        private String baseUrl = "http://discord.ser.mlanima.org:3001";
        private String apiSecret;
        private int invitePermissions = 76817;
    }
}
