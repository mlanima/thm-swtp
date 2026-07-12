package de.thm.swtp.api.discord.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds {@code discord.*} configuration properties from application.yml.
 * Groups the config into three nested namespaces: streams, bot, and oauth.
 */
@Component
@ConfigurationProperties(prefix = "discord")
@Getter
@Setter
public class DiscordProperties {

    private Streams streams = new Streams();
    private Bot bot = new Bot();
    private OAuth oauth = new OAuth();

    /**
     * Redis stream channel names and consumer tuning for Discord sync.
     */
    @Getter
    @Setter
    public static class Streams {
        private String outbound = "stream:discord:sync";
        private String inbound = "stream:platform:sync";
        private String consumerGroup = "spring-platform";
        private int batchSize = 10;
        private int blockMs = 1500;
        private int pendingTimeoutMs = 30000;
    }

    /**
     * Internal bot service endpoint and shared secret.
     */
    @Getter
    @Setter
    public static class Bot {
        private String baseUrl = "http://discord.ser.mlanima.org:3001";
        private String apiSecret;
        private int invitePermissions = 76817;
    }

    /**
     * Discord app OAuth2 credentials used for the bot add flow.
     */
    @Getter
    @Setter
    public static class OAuth {
        private String clientId;
        private String clientSecret;
        private String redirectUri;
    }
}
