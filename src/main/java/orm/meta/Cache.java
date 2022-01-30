package orm.meta;

import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class Cache {

    private final Map<Class<?>, Map<Object, Pair<Object, Object>>> cache = new HashMap<>();

    public <T> Optional<T> getEntityWithId(Class<T> entityType, Object id) {
        return getEntityPairWithId(entityType, id)
                .map(Pair::getLeft);
    }

    public <T> Optional<List<T>> getEntitiesWithIds(Class<T> entityType, List<Object> ids) {
        List<String> stringIds = ids.stream().map(Object::toString).toList();
        List<Object> entities = Optional.ofNullable(cache.get(entityType)).stream()
                .flatMap(it -> it.entrySet().stream())
                .filter(entry -> stringIds.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .map(Pair::getLeft)
                .toList();
        if (entities.size() == stringIds.size()) {
            return Optional.of((List<T>) entities);
        } else {
            return Optional.empty();
        }
    }

    @SneakyThrows
    public void setEntity(Entity entity, Object entityObject) {
        Object copy = entity.getType().getDeclaredConstructor().newInstance();
        entity.getAllFields().forEach(it -> invoke(it.getSetMethod(), copy, invoke(it.getMethod(), entityObject)));

        Optional.ofNullable(cache.get(entity.getType()))
                .orElseGet(() -> {
                    cache.put(entity.getType(), new HashMap<>());
                    return cache.get(entity.getType());
                }).put(getId(entity, entityObject).toString(), Pair.of(entityObject, copy));
    }

    public List<Field> getChangedEntityColumns(Object entityObject) {
        Entity entity = DslContext.TABLES.get(entityObject.getClass());
        Optional<? extends Pair<?, ?>> previous = getEntityPairWithId(entity.getType(), getId(entity, entityObject));

        if (previous.isEmpty()) {
            return emptyList();
        } else {
            return entity.getColumnFields().stream()
                    .filter(it -> !it.isForeignKey())
                    .filter(it -> !invoke(it.getMethod(), previous.get().getRight()).equals(invoke(it.getMethod(), entityObject)))
                    .toList();
        }
    }

    private <T> Optional<Pair<T, T>> getEntityPairWithId(Class<T> entityType, Object id) {
        return Optional.ofNullable(cache.get(entityType))
                .map(it -> (Pair<T, T>) it.get(id.toString()));
    }

    private Object getId(Entity entity, Object entityObject) {
        return invoke(entity.getPrimaryKeyField().getMethod(), entityObject);
    }

    @SneakyThrows
    private Object invoke(Method method, Object o, Object ...args) {
        return method.invoke(o, args);
    }
}
