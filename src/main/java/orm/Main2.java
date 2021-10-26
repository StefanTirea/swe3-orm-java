package orm;

import orm.meta.DslContext;
import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.sql.ConnectionPool;

public class Main2 {

    public static void main(String[] args) {
        ConnectionPool connectionPool = new ConnectionPool();
        DslContext dslContext = new DslContext();

        UserEntity userEntity = UserEntity.builder()
                .id(5L)
                .firstname("Stefandasd")
                .lastname("Test new Version")
                .age(10)
                .build();
        // Long save = dslContext.save(userEntity);
        LogEntity logEntity = LogEntity.builder()
                .entry("test")
                .user(userEntity)
                .build();
        Long save = dslContext.save(logEntity);
    }
}
