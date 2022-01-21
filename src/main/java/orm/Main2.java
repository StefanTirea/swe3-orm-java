package orm;

import org.apache.commons.lang3.tuple.Pair;
import orm.meta.DslContext;
import orm.sample.LogEntity;
import orm.sample.UserEntity;

import java.util.List;
import java.util.Optional;

public class Main2 {

    public static void main(String[] args) {
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
        Optional<UserEntity> byId = dslContext.findById(UserEntity.class, 1);
        List<UserEntity> byFirstname = dslContext.findBy(UserEntity.class, Pair.of("firstname", "Stefan"));
        //byId.orElseThrow().getLogs().add(logEntity);
    }
}
