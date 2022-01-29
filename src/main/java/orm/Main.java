package orm;

import lombok.SneakyThrows;
import orm.connection.ConnectionConfig;
import orm.meta.DslContext;
import orm.meta.Query;
import orm.sample.DatabaseConfig;
import orm.sample.LogEntity;
import orm.sample.Student;
import orm.sample.UserEntity;

import java.util.List;
import java.util.Optional;

public class Main {

    /*
    TODO: Join Table loop: both tables have onetomany/manyToOne
            Caching per Thread
            Where Queries group expression
            Support use of Optional<?> for ManyToOne
            Save 1:n & m:n => Find unsaved entities
            Column nullable not implemented
     */

    @SneakyThrows
    public static void main(String[] args) {
        DslContext dslContext = new DslContext(ConnectionConfig.builder()
                .connectionString(DatabaseConfig.getConfig().getConnectionString())
                .username(DatabaseConfig.getConfig().getUsername())
                .password(DatabaseConfig.getConfig().getPassword())
                .build());

        Student byId1 = dslContext.findById(Student.class, 1).orElseThrow();

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
        List<UserEntity> name = dslContext.findBy(UserEntity.class, Query.where()
                .equals("firstname", "Stefan")
                .and()
                .equals("lastname", "Tirea"));


        dslContext.findBy(UserEntity.class, Query.where().equals("id", 1));


        List<UserEntity> in = dslContext.findBy(UserEntity.class, Query.where()
                .not().in("firstname", "Stefan", "dsasdasaddsa"));
    }
}
