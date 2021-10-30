package orm;

import orm.meta.DslContext;
import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.sql.ConnectionPool;

import java.util.Optional;

public class Main2 {

    public static void main(String[] args) {
        ConnectionPool connectionPool = new ConnectionPool();
        DslContext dslContext = new DslContext();

        UserEntity userEntity = UserEntity.builder()
                .id(7L)
                .firstname("dsasdasaddsa")
                .lastname("Test new Version")
                .age(11)
                .build();
        dslContext.save(userEntity);
        LogEntity logEntity = LogEntity.builder()
                .entry("test")
                .user(userEntity)
                .build();
        // Long save = dslContext.save(logEntity);
        Optional<LogEntity> byId = dslContext.findById(LogEntity.class, 1);
        //byId.orElseThrow().getLogs().add(logEntity);
        UserEntity user = byId.orElseThrow().getUser();
        System.out.println(user.getFirstname());
    }
}
