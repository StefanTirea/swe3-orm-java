package orm.meta;

import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.collections4.ListUtils;
import org.postgresql.jdbc.PgArray;
import orm.annotation.Table;
import orm.connection.ConnectionConfig;
import orm.connection.ConnectionPool;
import orm.sample.entity.Teacher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.function.Predicate.not;

@Slf4j
public class DslContext {

    static final Map<Class<?>, Entity> TABLES = Collections.synchronizedMap(new HashMap<>());
    private static final ThreadLocal<Cache> CACHE = ThreadLocal.withInitial(Cache::new);
    private final ConnectionPool connectionPool;

    public DslContext(ConnectionConfig config) {
        this.connectionPool = new ConnectionPool(config);
    }

    @SneakyThrows
    public <T> Optional<T> findById(Class<T> type, Object id) {
        log.debug("findById {} {}", type, id);
        Optional<T> cachedEntity = CACHE.get().getEntityWithId(type, id);
        return cachedEntity.isPresent()
                ? cachedEntity
                : findFirstBy(type, Query.where().equals(getEntityForClass(type).getPrimaryKeyField().getColumnName(), id));
    }

    public <T> Optional<T> findFirstBy(Class<T> type, Query query) {
        return findBy(type, query).stream().findFirst();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findBy(Class<T> type, Query query) {
        log.debug("findBy with query {} {}", type, query);
        Entity entity = getEntityForClass(type);
        QueryResult whereAndValues = Optional.ofNullable(query).map(Query::build).orElse(QueryResult.empty());

        String columns = entity.getAllFields().stream()
                .filter(not(Field::isVirtualColumn))
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));

        String selectQuery = String.format("SELECT %s FROM %s %s", columns, entity.getTableName(), whereAndValues.getWhereQuery());

        return (List<T>) mapObjectsFromResultSet(entity, whereAndValues.getValues(), selectQuery);
    }

    @SneakyThrows
    public <T> boolean delete(T entityValue) {
        Entity entity = getEntityForObject(entityValue);
        Object id = entity.getPrimaryKeyField().getColumnValue(entityValue);

        if (id == null) {
            return false;
        }
        String insertQuery = String.format("DELETE FROM %s WHERE %s = ?", entity.getTableName(), entity.getPrimaryKeyField().getColumnName());

        return executePreparedStatement(List.of(id), insertQuery) != null;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> findAll(Class<T> type) {
        return findBy(type, null);
    }

    @SuppressWarnings("unchecked")
    public <T> boolean deleteAll(List<T> entityValues) {
        return entityValues.stream().allMatch(this::delete);
    }

    public Object save(Object object) {
        return save(object, true);
    }

    @SneakyThrows
    private Object save(Object object, boolean checkDependencies) {
        Entity entity = getEntityForObject(object);
        Object id = entity.getPrimaryKeyField().getMethod().invoke(object) == null
                ? insert(entity, object, true)
                : update(entity, object, checkDependencies);
        if (id != null) {
            // TODO make new copy with ID and retun this object instead
            entity.getPrimaryKeyField().getSetMethod().invoke(object, id);
            CACHE.get().setEntity(entity, object);
        }
        return id;
    }

    @SneakyThrows
    private Object update(Entity entity, Object object, boolean checkDependencies) {
        checkDependencies(entity, object, checkDependencies);
        List<Field> changedColumns = CACHE.get().getChangedEntityColumns(object);
        if (changedColumns.isEmpty()) {
            if (log.isTraceEnabled()) {
                log.trace("Update Entity {} with value {} has no changes", entity.getType(), object);
            }
            return entity.getPrimaryKeyField().getColumnValue(object);
        }

        List<Object> values = ListUtils.union(changedColumns.stream().map(it -> it.getColumnValue(object)).toList(),
                List.of(entity.getPrimaryKeyField().getColumnValue(object)));
        if (log.isDebugEnabled()) {
            log.debug("Changes detected for Entity {} with new values {}", entity.getType(), values);
        }
        String columns = changedColumns.stream()
                .map(field -> field.getColumnName() + " = ?")
                .collect(Collectors.joining(","));

        String updateQuery = String.format("UPDATE %s SET %s WHERE %s = ?", entity.getTableName(), columns, entity.getPrimaryKeyField().getColumnName());

        return executePreparedStatement(values, updateQuery);
    }

    @SneakyThrows
    private Object insert(Entity entity, Object object, boolean checkDependencies) {
        checkDependencies(entity, object, checkDependencies);
        String columns = entity.getColumnFields().stream()
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));
        String prepareColumns = IntStream.range(0, entity.getColumnFields().size())
                .mapToObj(it -> "?")
                .collect(Collectors.joining(","));

        String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s)", entity.getTableName(), columns, prepareColumns);

        return executePreparedStatement(entity.getColumnValues(object), insertQuery);
    }

    private void checkDependencies(Entity entity, Object object, boolean checkDependencies) {
        if (checkDependencies) {
            entity.getForeignKeys().stream()
                    .map(it -> it.invokeGetMethod(object))
                    .forEach(it -> save(it, false));

            entity.getVirtualFields().stream()
                    .map(it -> ((List<?>) it.invokeGetMethod(object)))
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .forEach(it -> save(it, false));
        }
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
                    mapColumnForEntity(rs, o, column, entity);
                }
                objects.add(o);
                CACHE.get().setEntity(entity, o);
            }
            connection().commit();
            return objects;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SneakyThrows
    private List<Object> selectIds(String query, Object id) {
        try {
            @Cleanup PreparedStatement preparedStatement = connection().prepareStatement(query);
            preparedStatement.setObject(1, id);
            @Cleanup ResultSet rs = preparedStatement.executeQuery();
            List<Object> objects = new ArrayList<>();
            while (rs.next()) {
                objects.add(rs.getObject(1));
            }
            return objects;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private void mapColumnForEntity(ResultSet rs, Object o, Field column, Entity entity) throws SQLException {
        try {
            if (column.isForeignKey()) {
                mapForeignKeyColumn(rs, o, column);
            } else if (column.isVirtualColumn()) {
                mapJoinColumn(rs, o, column, entity);
            } else {
                // default behaviour if normal column
                Object columnValue = rs.getObject(column.getColumnName());
                column.getSetMethod().invoke(o, convertObject(columnValue));
                // Cache Object reference immediately after primary key is set if foreignKey or virtual columns reference same object
                if (column.isPrimaryKey()) {
                    CACHE.get().setEntity(entity, o);
                }
            }
        } catch (InvocationTargetException | IllegalAccessException e) {
            log.error("An error happened while invoking the get/set method for {} {}", entity, column, e);
            throw new RuntimeException(e);
        }
    }

    @SneakyThrows
    private Object convertObject(Object columnValue) {
        if (columnValue == null) {
            return columnValue;
        }
        if (columnValue instanceof PgArray) {
            return ((PgArray) columnValue).getArray();
        } else if (columnValue instanceof Timestamp) {
            return ((Timestamp) columnValue).toLocalDateTime();
        } else if (columnValue instanceof Date) {
            return ((Date) columnValue).toLocalDate();
        }
        return columnValue;
    }

    /**
     * Handle when column is foreignKey
     *
     * @param rs
     * @param o
     * @param column
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void mapForeignKeyColumn(ResultSet rs, Object o, Field column) throws SQLException, IllegalAccessException, InvocationTargetException {
        Object id = rs.getObject(column.getColumnName());
        Optional<?> cachedEntity = CACHE.get().getEntityWithId(column.getType(), id);

        if (cachedEntity.isPresent()) {
            column.getSetMethod().invoke(o, createLazyProxy(column.getType(), () -> CACHE.get().getEntityWithId(column.getType(), id).orElseThrow()));
        } else if (column.isLazy()) {
            var lazyColumnValue = createLazyProxy(column.getType(), () -> findById(column.getType(), id).orElseThrow());
            column.getSetMethod().invoke(o, lazyColumnValue);
        } else {
            column.getSetMethod().invoke(o, findById(column.getType(), id).orElseThrow());
        }
    }

    /**
     * Handle when column is used for joining only
     *
     * @param rs
     * @param o
     * @param column
     * @param entity
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
    private void mapJoinColumn(ResultSet rs, Object o, Field column, Entity entity) throws SQLException, IllegalAccessException, InvocationTargetException {
        Class<?> joinType = column.getSubType();
        String fkColumnName = column.getColumnName();
        Object id = rs.getObject(entity.getPrimaryKeyField().getColumnName());
        Entity joinEntity = getEntityForClass(joinType);
        String joinEntityPkColumnName = joinEntity.getPrimaryKeyField().getColumnName();

        Optional<? extends List<?>> cachedEntities;
        Supplier<?> joinQuery;
        List<Object> joinIds;

        // Check Cache and create Supplier for Join Select for ManyToMany or OneToMany
        if (column.isManyToMany()) {
            joinIds = getJoinIdsForManyToManyColumn(column, joinType, fkColumnName, id);
            cachedEntities = CACHE.get().getEntitiesWithIds(joinType, joinIds);
            joinQuery = () -> findBy(joinType, Query.where().in(joinEntityPkColumnName, joinIds));
        } else {
            String query = String.format("SELECT %s FROM %s where %s = ?", joinEntityPkColumnName, joinEntity.getTableName(), fkColumnName);
            joinIds = selectIds(query, id);

            cachedEntities = CACHE.get().getEntitiesWithIds(joinType, joinIds);
            joinQuery = () -> findBy(joinType, Query.where().in(joinEntityPkColumnName, joinIds));
        }

        // If cached is present then use otherwise check if lazy or eager
        if (cachedEntities.isPresent()) {
            column.getSetMethod().invoke(o, createLazyProxy(column.getType(), () -> CACHE.get().getEntitiesWithIds(joinType, joinIds).orElseThrow()));
        } else if (column.isLazy()) {
            Object lazyColumnValue = createLazyProxy(column.getType(), joinQuery);
            column.getSetMethod().invoke(o, lazyColumnValue);
        } else {
            column.getSetMethod().invoke(o, joinQuery.get());
        }
    }

    private List<Object> getJoinIdsForManyToManyColumn(Field column, Class<?> joinType, String fkColumnName, Object id) {
        String table = column.getManyToManyTable();
        Entity joinEntity = getEntityForClass(joinType);
        String fkJoinEntityColumnName = joinEntity.getVirtualFields().stream()
                .filter(it -> it.isManyToMany() && it.getSubType().equals(column.getEntity().getType()))
                .findFirst()
                .map(Field::getColumnName)
                .orElseThrow(() -> new RuntimeException("ManyToMany ForeignKey is not set in Entity " + joinEntity.getType()));

        String query = String.format("SELECT %s FROM %s WHERE %s = ?", fkJoinEntityColumnName, table, fkColumnName);
        return selectIds(query, id);
    }

    private Entity getEntityForClass(Class<?> type) {
        Class<?> realType = type;
        if (type == null) {
            throw new IllegalArgumentException();
        } else if (!type.isAnnotationPresent(Table.class)) {
            realType = type.getSuperclass();
        }
        if (!TABLES.containsKey(realType)) {
            TABLES.put(realType, new Entity(realType));
        }
        return TABLES.get(realType);
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
                .load(Teacher.class.getClassLoader())
                .getLoaded()
                .getConstructor().newInstance();
    }

    @SneakyThrows
    private Object invoke(Method method, Object o, Object ...args) {
        return method.invoke(o, args);
    }
}
