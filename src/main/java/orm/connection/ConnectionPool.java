package orm.connection;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
public class ConnectionPool {

    private final Deque<Connection> pool = new ArrayDeque<>();
    private final List<Connection> allConnections = new ArrayList<>();
    private final ConnectionConfig config;

    public ConnectionPool(ConnectionConfig connectionConfig) {
        this.config = connectionConfig;
        initConnections();
        Runtime.getRuntime().addShutdownHook(new Thread(this::closeConnections));
    }

    public Connection getConnection() {
        if (ConnectionContext.CONNECTION.get() == null) {
            synchronized (pool) {
                if (pool.isEmpty()) {
                    Connection connection = createConnection();
                    pool.push(connection);
                }
                ConnectionContext.CONNECTION.set(pool.pop());
            }
        }
        return ConnectionContext.CONNECTION.get();
    }

    @SneakyThrows
    public void commitTransaction() {
        ConnectionContext.CONNECTION.get().commit();
    }

    @SneakyThrows
    public void rollbackTransaction() {
        ConnectionContext.CONNECTION.get().rollback();
    }

    @SneakyThrows
    public void releaseConnection() {
        Connection connection = ConnectionContext.CONNECTION.get();
        ConnectionContext.CONNECTION.remove();
        if (connection.isClosed()) {
            log.warn("Connection was already closed!");
            allConnections.remove(connection);
        } else {
            connection.rollback();
            synchronized (pool) {
                pool.add(connection);
            }
        }
    }

    @SneakyThrows
    private void initConnections() {
        IntStream.range(0, config.getMinIdleConnections())
                .forEach(i -> pool.push(createConnection()));
    }

    @SneakyThrows
    private Connection createConnection() {
        Connection connection = DriverManager.getConnection(config.getConnectionString(), config.getUsername(), config.getPassword());
        allConnections.add(connection);
        connection.setAutoCommit(false);
        return connection;
    }

    @SneakyThrows
    private void closeConnections() {
        for (Connection connection : allConnections) {
            connection.close();
        }
    }
}
