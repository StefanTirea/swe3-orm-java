package orm.sql;

import orm.config.Entity;
import orm.sample.UserEntity;

public class UserRepository extends Repository<UserEntity> {

    public UserRepository() {
        super(UserEntity.class);
    }
}
