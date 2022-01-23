package orm;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import orm.meta.DslContext;
import orm.meta.Query;
import orm.sample.DatabaseConfig;
import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.connection.ConnectionConfig;
import orm.connection.ConnectionPool;

import java.util.List;
import java.util.Optional;

public class Main2 {

    /*
    TODO: Join Table loop: both tables have onetomany/manyToOne
            Caching per Thread
            Fluent API for where Queries
            Support use of Optional<?> for ManyToOne
            Save 1:n & m:n => Find unsaved entities
     */

    @SneakyThrows
    public static void main(String[] args) {
        DslContext dslContext = new DslContext(ConnectionConfig.builder()
                .connectionString(DatabaseConfig.getConfig().getConnectionString())
                .username(DatabaseConfig.getConfig().getUsername())
                .password(DatabaseConfig.getConfig().getPassword())
                .build());

        UserEntity userEntity = UserEntity.builder()
                .id(2L)
                .firstname("dsasdasaddsa")
                .lastname("Test new Version")
                .age(11)
                .build();
        Optional<UserEntity> save = dslContext.save(userEntity);
        LogEntity logEntity = LogEntity.builder()
                .entry("test")
                .user(userEntity)
                .build();
        // Long save = dslContext.save(logEntity);
        Optional<UserEntity> byId = dslContext.findById(UserEntity.class, 1);
        List<UserEntity> byFirstname = dslContext.findBy(UserEntity.class, Query.where("firstname", "Stefan").and("lastname", "Tirea"));
        //byId.orElseThrow().getLogs().add(logEntity);
        dslContext.save(byId.get());
    }
}
