package orm.meta;

import lombok.SneakyThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Cache {

    private final Map<Class<?>, Map<Object, Object>> cache = new HashMap<>();

    public <T> Optional<T> getEntityWithId(Class<T> entityType, Object id) {
        return Optional.ofNullable(cache.get(entityType))
                .map(it -> (T) it.get(id.toString()));
    }

    public <T> Optional<List<T>> getEntitiesWithIds(Class<T> entityType, List<Object> ids) {
        List<String> stringIds = ids.stream().map(Object::toString).toList();
        List<Object> entities = Optional.ofNullable(cache.get(entityType)).stream()
                .flatMap(it -> it.entrySet().stream())
                .filter(entry -> stringIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();
        if (entities.size() == stringIds.size()) {
            return Optional.of((List<T>) entities);
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public void setEntity(Entity entity, Object entityObject) {
        Optional.ofNullable(cache.get(entity.getType()))
                .orElseGet(() -> {
                    cache.put(entity.getType(), new HashMap<>());
                    return cache.get(entity.getType());
                }).put(entity.getPrimaryKeyField().getMethod().invoke(entityObject).toString(), entityObject);
    }

    @SneakyThrows
    public void setEntities(Entity entity, List<Object> entityObjects) {
        Map<Object, Object> entityMap = Optional.ofNullable(cache.get(entity.getType()))
                .orElseGet(() -> {
                    cache.put(entity.getType(), new HashMap<>());
                    return cache.get(entity.getType());
                });
        for (Object entityObject : entityObjects) {
            entityMap.put(entity.getPrimaryKeyField().getMethod().invoke(entityObject).toString(), entityObject);
        }
    }
}
