package orm.meta;

import lombok.Cleanup;
import lombok.SneakyThrows;
import org.reflections.Reflections;
import orm.annotation.Table;
import orm.sql.ConnectionContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

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

    // public <T> T save(T entity) {
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <T extends Number> T save(Object entity) {
        Entity table = getEntityForObject(entity);

        String columns = table.getColumnFields().stream()
                .map(Field::getColumnName)
                .collect(Collectors.joining(","));
        String prepareColumns = "?,".repeat(table.getColumnFields().size());

        String insertQuery = String.format("INSERT INTO %s (%s) VALUES (%s)", table.getTableName(), columns, prepareColumns.substring(0, prepareColumns.length()-1));

        try {
            @Cleanup PreparedStatement preparedStatement = connection().prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS);
            List<Object> columnValues = table.getColumnValues(entity);
            for (int i = 1; i < table.getColumnFields().size() + 1; i++) {
                preparedStatement.setObject(i, columnValues.get(i - 1));
            }
            preparedStatement.execute();
            @Cleanup ResultSet rs = preparedStatement.getGeneratedKeys();
            rs.next();
            T id = (T) table.getPrimaryKeyField().getType().cast(rs.getObject(1));
            connection().commit();
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw e;
        }
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
