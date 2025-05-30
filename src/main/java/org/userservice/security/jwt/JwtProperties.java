package org.userservice.security.jwt;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    private boolean useRefresh = false;
    private Access access = new Access();
    private Refresh refresh = new Refresh();

    @Data
    static class Access {
        private String secret = "default_access_secret";
        private Duration expiration = Duration.ofMinutes(30);
    }

    @Data
    static class Refresh {
        private String secret = "default_refresh_secret";
        private Duration expiration = Duration.ofDays(7);
    }

    public boolean isUseRefresh() {
        return useRefresh;
    }

    public void setUseRefresh(boolean useRefresh) {
        this.useRefresh = useRefresh;
    }
}
