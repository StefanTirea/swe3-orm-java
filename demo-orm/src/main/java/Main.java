import orm.connection.ConnectionConfig;
import orm.meta.DslContext;
import orm.sample.DatabaseConfig;
import service.DemoService;

public class Main {

    public static void main(String[] args) {
        DemoService app = new DemoService(new DslContext(ConnectionConfig.builder()
                .connectionString(DatabaseConfig.getConfig().getConnectionString())
                .username(DatabaseConfig.getConfig().getUsername())
                .password(DatabaseConfig.getConfig().getPassword())
                .build()));

        app.start();
    }
}
