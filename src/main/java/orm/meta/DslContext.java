package orm.meta;

import lombok.Cleanup;
import lombok.SneakyThrows;
import net.sf.cglib.proxy.Enhancer;
import org.apache.commons.lang3.tuple.Pair;
import org.reflections.Reflections;
import orm.annotation.Table;
import orm.sample.LazyLoading;
import orm.sample.LazyObjectHandler;
import orm.sql.ConnectionContext;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

public class DslContext {

    private static Map<Class<?>, Entity> TABLES;

    public DslContext() {
        if (TABLES == null) {
            TABLES = new HashMap<>();
            Reflections reflections = new Reflections("");
            Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Table.class);
            typesAnnotatedWith.forEach(clazz -> TABLES.put(clazz, new Entity(clazz)));
        }
    }

    @SneakyThrows
    public <T> Optional<T> findById(Class<T> type, Object id) {
        System.out.println("findById");
        Entity entity = getEntityForClass(type);

        String columns = entity.getAllFields().stream()
                .filter(not(Field::isVirtualColumn))
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));

        String selectQuery = String.format("SELECT %s FROM %s WHERE %s = ?", columns, entity.getTableName(), entity.getPrimaryKeyField().getColumnName());

        return mapObjectsFromResultSet(entity, List.of(id), selectQuery).stream()
                .findFirst()
                .map(type::cast);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> List<T> findBy(Class<T> type, Pair<String, Object>... where) {
        System.out.println("findBy");
        Entity entity = getEntityForClass(type);

        String columns = entity.getAllFields().stream()
                .filter(not(Field::isVirtualColumn))
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));
        String whereQuery = Arrays.stream(where)
                .map(pair -> pair.getLeft() + " = ?")
                .collect(Collectors.joining(","));

        String selectQuery = String.format("SELECT %s FROM %s WHERE %s", columns, entity.getTableName(), whereQuery);

        return (List<T>) mapObjectsFromResultSet(entity, Arrays.stream(where).map(Pair::getRight).collect(Collectors.toList()), selectQuery);
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T> T save(T object) {
        Entity entity = getEntityForObject(object);
        Object id = entity.getPrimaryKeyField().getMethod().invoke(object) == null
                ? insert(entity, object)
                : update(entity, object);
        return (T) findById(entity.getType(), id).orElseThrow();
    }

    @SneakyThrows
    private Object update(Entity entity, Object object) {
        String columns = entity.getColumnFields().stream()
                .map(field -> field.getColumnName() + " = ?")
                .collect(Collectors.joining(","));

        String updateQuery = String.format("UPDATE %s SET %s WHERE %s = ?", entity.getTableName(), columns, entity.getPrimaryKeyField().getColumnName());

        return executePreparedStatement(Stream.of(entity.getColumnValues(object), List.of(entity.getPrimaryKeyField().getMethod().invoke(object)))
                .flatMap(Collection::stream)
                .collect(Collectors.toList()), updateQuery);
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

    private Object executePreparedStatement(List<Object> values, String query) throws SQLException {
        try {
            @Cleanup PreparedStatement preparedStatement = connection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            for (int i = 1; i <= values.size(); i++) {
                preparedStatement.setObject(i, values.get(i - 1));
            }
            preparedStatement.execute();
            @Cleanup ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            Object id = rs.getObject(1);
            connection().commit();
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    private List<Object> mapObjectsFromResultSet(Entity entity, List<Object> values, String query) {
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
                    if (column.isForeignKey()) {
                        Object id = rs.getObject(column.getColumnName());
                        if (column.isLazy()) {
                            column.getSetMethod().invoke(o, Enhancer.create(column.getType(), new LazyLoading<>(() -> findById(column.getType(), id).orElseThrow())));
                        } else {
                            column.getSetMethod().invoke(o, findById(column.getType(), id).orElseThrow());
                        }
                    } else if (column.isVirtualColumn()) {
                        Field fkField = getEntityForClass(column.getType()).getFieldByClass(entity.getType());
                        Object id = rs.getObject(entity.getPrimaryKeyField().getColumnName());
                        if (column.isLazy()) {
                            var invoker = new LazyObjectHandler(() -> findBy(fkField.getEntity().getType(), Pair.of(fkField.getColumnName(), id)), value -> {
                                try {
                                    column.getSetMethod().invoke(o, value);
                                } catch (IllegalAccessException | InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                            });
                            Object columnValue = Proxy.newProxyInstance(column.getType().getClassLoader(), new Class[]{column.getType()}, invoker);
                            column.getSetMethod().invoke(o, columnValue);
                        } else {
                            column.getSetMethod().invoke(o, findBy(fkField.getEntity().getType(), Pair.of(fkField.getColumnName(), id)));
                        }
                    } else {
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
        if (type == null || !TABLES.containsKey(type)) {
            throw new IllegalArgumentException();
        }
        return TABLES.get(type);
    }

    private Entity getEntityForObject(Object o) {
        if (o == null || !TABLES.containsKey(o.getClass())) {
            throw new IllegalArgumentException();
        }
        return TABLES.get(o.getClass());
    }

    private Connection connection() {
        return ConnectionContext.CONNECTION.get();
    }
}
