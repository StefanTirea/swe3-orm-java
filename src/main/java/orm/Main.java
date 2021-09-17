package orm;

import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.sql.ConnectionPool;
import orm.sql.UserRepository;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        ConnectionPool connectionPool = new ConnectionPool();
        connectionPool.setConnection();

        ConfigGenerator configGenerator = new ConfigGenerator();
        UserRepository userRepository = new UserRepository();
        List<UserEntity> userEntities = userRepository.selectAll();
        List<UserEntity> ids = userRepository.select("id", "entry")
                .from(UserEntity.class)
                .join(LogEntity.class)
                .fetchAll();

        connectionPool.releaseConnection(false);
    }
}
