package orm;

import lombok.SneakyThrows;
import orm.connection.ConnectionConfig;
import orm.meta.DslContext;
import orm.sample.DatabaseConfig;
import orm.sample.LogEntity;
import orm.sample.UserEntity;

public class Main {

    /*
    TODO:
            Insert/Update 1:n & m:n => Find unsaved entities
            Unit Test
            Test Application with Spring Boot
            Caching on Save/Update
            Transaction

            ✕ Cascading Delete => Must be done via SQL Create Table
            ✓ Join Table loop: both tables have onetomany/manyToOne
            ✓ Caching per Thread
            ✕ Where Queries group expression
            ✕ Support use of Optional<?> for ManyToOne
            ✕ Column nullable not implemented
     */

    @SneakyThrows
    public static void main(String[] args) {
        DslContext dslContext = new DslContext(ConnectionConfig.builder()
                .connectionString(DatabaseConfig.getConfig().getConnectionString())
                .username(DatabaseConfig.getConfig().getUsername())
                .password(DatabaseConfig.getConfig().getPassword())
                .build());

        var byId1 = dslContext.findById(UserEntity.class, 1).orElseThrow();

        byId1.setAge(99);
        //byId1.getLogs().get(0).setEntry("cascade update working!");

        // Cache und diese Entity haben selbe reference im cache
        dslContext.save(byId1);

        System.out.println();
        /*UserEntity userEntity = UserEntity.builder()
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
                .not().in("firstname", "Stefan", "dsasdasaddsa"));*/
    }
}
