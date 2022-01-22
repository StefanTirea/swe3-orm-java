package orm;

import lombok.SneakyThrows;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.tuple.Pair;
import orm.meta.DslContext;
import orm.sample.LazyLoadingInterceptor;
import orm.sample.LogEntity;
import orm.sample.UserEntity;
import orm.sql.ConnectionPool;

import java.util.List;
import java.util.Optional;

public class Main2 {

    @SneakyThrows
    public static void main(String[] args) {
        // sets connection in local thread
        ConnectionPool connectionPool = new ConnectionPool();
        DslContext dslContext = new DslContext();

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
        List<UserEntity> byFirstname = dslContext.findBy(UserEntity.class, Pair.of("firstname", "Stefan"));
        //byId.orElseThrow().getLogs().add(logEntity);
        dslContext.save(byId.get());
    }
}
