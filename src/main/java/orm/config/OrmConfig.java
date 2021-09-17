package orm.config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrmConfig {

    public static final Map<Class<?>, Entity> ORM_CONFIG = new HashMap<>();

    public static Entity getConfigForEntity(Class<?> clazz) {
        return ORM_CONFIG.get(clazz);
    }
}
