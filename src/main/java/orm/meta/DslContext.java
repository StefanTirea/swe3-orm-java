package orm.meta;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.tuple.Pair;
import orm.connection.ConnectionConfig;
import orm.connection.ConnectionPool;
import orm.sample.LazyLoadingInterceptor;
import orm.sample.LogEntity;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

@Slf4j
public class DslContext {

    private final Map<Class<?>, Entity> TABLES = Collections.synchronizedMap(new HashMap<>());
    private final ConnectionPool connectionPool;

    public DslContext(ConnectionConfig config) {
        this.connectionPool = new ConnectionPool(config);
    }

    @SneakyThrows
    public <T> Optional<T> findById(Class<T> type, Object id) {
        log.debug("findById {} {}", type, id);
        return findFirstBy(type, Query.where(getEntityForClass(type).getPrimaryKeyField().getColumnName(), id));
    }

    public <T> Optional<T> findFirstBy(Class<T> type, Query query) {
        return findBy(type, query).stream().findFirst();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findBy(Class<T> type, Query query) {
        log.debug("findBy with query {} {}", type, query);
        Entity entity = getEntityForClass(type);
        List<Pair<String, Object>> where = query.build(entity);

        String columns = entity.getAllFields().stream()
                .filter(not(Field::isVirtualColumn))
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));
        String whereQuery = where.stream()
                .map(pair -> pair.getLeft() + " = ?")
                .collect(Collectors.joining(" and "));

        String selectQuery = String.format("SELECT %s FROM %s WHERE %s", columns, entity.getTableName(), whereQuery);

        return (List<T>) mapObjectsFromResultSet(entity, where.stream().map(Pair::getRight).toList(), selectQuery);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> Optional<T> save(T object) {
        Entity entity = getEntityForObject(object);
        Object id = entity.getPrimaryKeyField().getMethod().invoke(object) == null
                ? insert(entity, object)
                : update(entity, object);
        return Optional.ofNullable(id).flatMap(it -> findById(entity.getType(), it)).map(it -> (T) it);
    }

    @SneakyThrows
    private Object update(Entity entity, Object object) {
        String columns = entity.getColumnFields().stream()
                .map(field -> field.getColumnName() + " = ?")
                .collect(Collectors.joining(","));

        String updateQuery = String.format("UPDATE %s SET %s WHERE %s = ?", entity.getTableName(), columns, entity.getPrimaryKeyField().getColumnName());

        return executePreparedStatement(Stream.of(entity.getColumnValues(object), List.of(entity.getPrimaryKeyField().getMethod().invoke(object)))
                .flatMap(Collection::stream)
                .toList(), updateQuery);
    }

    @SneakyThrows
    private Object insert(Entity entity, Object object) {
        String columns = entity.getColumnFields().stream()
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));
        String prepareColumns = IntStream.range(0, entity.getColumnFields().size())
                .mapToObj(it -> "?")
                .collect(Collectors.joining(","));

        String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s)", entity.getTableName(), columns, prepareColumns);

        return executePreparedStatement(entity.getColumnValues(object), insertQuery);
    }

    /**
     * @param values   for prepared statement
     * @param sqlQuery to be run
     * @return Id from updated entity or null when nothing was updated
     * @throws SQLException on sql error
     */
    private Object executePreparedStatement(List<Object> values, String sqlQuery) throws SQLException {
        try {
            @Cleanup PreparedStatement preparedStatement = connection().prepareStatement(sqlQuery, Statement.RETURN_GENERATED_KEYS);
            for (int i = 1; i <= values.size(); i++) {
                preparedStatement.setObject(i, values.get(i - 1));
            }
            preparedStatement.execute();
            @Cleanup ResultSet rs = preparedStatement.getGeneratedKeys();
            Object id = null;
            if (rs.next()) {
                id = rs.getObject(1);
            }
            connection().commit();
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SneakyThrows
    private List<Object> mapObjectsFromResultSet(Entity entity, List<Object> values, String query) {
        log.debug("Query {} for entity {} with values {}", query, entity, values);
        try {
            @Cleanup PreparedStatement preparedStatement = connection().prepareStatement(query);
            for (int i = 1; i <= values.size(); i++) {
                preparedStatement.setObject(i, values.get(i - 1));
            }
            @Cleanup ResultSet rs = preparedStatement.executeQuery();
            List<Object> objects = new ArrayList<>();
            while (rs.next()) {
                Object o = entity.getType().getDeclaredConstructor().newInstance();
                for (Field column : entity.getAllFields()) {
                    if (column.isForeignKey()) { // handle when column is foreignKey
                        Object id = rs.getObject(column.getColumnName());
                        if (column.isLazy()) {
                            var lazyColumnValue = createLazyProxy(column.getType(), () -> findById(column.getType(), id).orElseThrow());
                            column.getSetMethod().invoke(o, lazyColumnValue);
                        } else {
                            column.getSetMethod().invoke(o, findById(column.getType(), id).orElseThrow());
                        }
                    } else if (column.isVirtualColumn()) { // handle when column is used for joining only
                        Field fkField = getEntityForClass(column.getType()).getFieldByClass(entity.getType());
                        Object id = rs.getObject(entity.getPrimaryKeyField().getColumnName());
                        if (column.isLazy()) {
                            Object lazyColumnValue = createLazyProxy(fkField.getEntity().getType(), () -> findBy(fkField.getEntity().getType(), Query.where(fkField.getColumnName(), id)));
                            column.getSetMethod().invoke(o, lazyColumnValue);
                        } else {
                            column.getSetMethod().invoke(o, findBy(fkField.getEntity().getType(), Query.where(fkField.getColumnName(), id)));
                        }
                    } else { // default behaviour if normal column
                        Object columnValue = rs.getObject(column.getColumnName());
                        column.getSetMethod().invoke(o, columnValue);
                    }
                }
                objects.add(o);
            }
            connection().commit();
            return objects;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private Entity getEntityForClass(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException();
        } else if (!TABLES.containsKey(type)) {
            TABLES.put(type, new Entity(type));
        }
        return TABLES.get(type);
    }

    private Entity getEntityForObject(Object o) {
        if (o == null) {
            throw new IllegalArgumentException();
        }
        return getEntityForClass(o.getClass());
    }

    private Connection connection() {
        return connectionPool.getConnection();
    }

    @SneakyThrows
    private Object createLazyProxy(Class<?> type, Supplier<?> lazySupplier) {
        return new ByteBuddy()
                .subclass(type)
                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(new LazyLoadingInterceptor(lazySupplier)))
                .make()
                .load(LogEntity.class.getClassLoader())
                .getLoaded()
                .getConstructor().newInstance();
    }
}
