package orm.sql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import orm.config.Entity;
import orm.config.Field;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static orm.config.OrmConfig.ORM_CONFIG;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Query<T> {

    private Entity entity;
    private Supplier<List<Field>> supplier = Collections::emptyList;
    private QueryConfig query = new QueryConfig();

    public static Query selectFrom(Class<?> clazz) {
        Query query = new Query();
        query.from(clazz);
        query.entity.getFields().forEach(field -> query.query.getSelect().put(field.getName(), field));
        return query;
    }

    public static Query select(String... columns) {
        Query query = new Query();
        List<String> fields = List.of(columns);
        query.supplier = () -> query.entity.getFields().stream().filter(field -> fields.contains(field.getName())).collect(Collectors.toList());
        return query;
    }

    public Query<T> from(Class<?> clazz) {
        this.entity = ORM_CONFIG.computeIfAbsent(clazz, this::throwException);
        this.query.setFrom(this.entity);
        supplier.get().forEach(field -> query.getSelect().put(field.getName(), field));
        return this;
    }

    public Query<T> join(Class<?> clazz) {
        this.query.getJoin().add(Join.builder()
                        .left(nonNull(query.getFrom()) ? query.getFrom() : query.getJoin().get(query.getJoin().size()-1).getRight())
                        .right(ORM_CONFIG.get(clazz)) // TODO check if this join is also in the config of the from entity
                        .type(JoinType.INNER_JOIN)
                .build());
        return this;
    }

    @SneakyThrows
    public List<T> fetchAll() {
        return (List<T>) execute(this::convertToEntity);
    }

    @SneakyThrows
    private Object convertToEntity(ResultSet rs) {
        List<T> result = new ArrayList<>();
        while(rs.next()) {
            query.getJoin().stream()
                    .flatMap(join -> join.getRight().getFields().stream())
                    .filter(field -> query.getSelect().containsKey(field.getName()))

            Object o = entity.getType().getDeclaredConstructor().newInstance();
            for(Field f : query.getSelect()) {
                java.lang.reflect.Field declaredField = entity.getType().getDeclaredField(f.getName());
                declaredField.setAccessible(true);
                declaredField.set(o, rs.getObject(f.getName()));
            }
            result.add((T)o);
        }
        return result;
    }

    private Object execute(Function<ResultSet, ?> fn) {
        try (PreparedStatement preparedStatement = connection().prepareStatement(query.buildQuery())) {
            return fn.apply(preparedStatement.executeQuery());
        } catch (SQLException e) {
            return null;
        }
    }

    private Connection connection() {
        return ConnectionContext.CONNECTION.get();
    }

    private Entity throwException(Class<?> clazz) {
        throw new IllegalArgumentException(clazz.getName() + " is not an Entity!");
    }
}
