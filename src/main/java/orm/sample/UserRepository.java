package orm.sample;

import orm.annotation.Repository;

@Repository
public interface UserRepository {

    UserEntity findByFirstname(String firstname);
}
