package orm.connection;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Builder
@Getter
public class ConnectionConfig {

    private final String connectionString;
    private final String username;
    private final String password;
    private Integer minIdleConnections = 1;
    private Integer maxIdleConnections = 5;

    @Builder
    public ConnectionConfig(String connectionString, String username, String password,
                            Integer minIdleConnections, Integer maxIdleConnections) {
        if (ObjectUtils.anyNull(connectionString, username, password)) {
            throw new IllegalStateException("connectionString, username or password can not be null");
        }
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;

        if (minIdleConnections != null && minIdleConnections > 1) {
            this.minIdleConnections = minIdleConnections;
        }
        if (maxIdleConnections != null && maxIdleConnections > 5) {
            this.maxIdleConnections = maxIdleConnections;
        }
    }
}
