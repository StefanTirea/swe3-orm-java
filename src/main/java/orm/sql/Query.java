package orm.sql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import orm.config.Entity;
import orm.config.EntityRelation;
import orm.config.Field;
import orm.config.RelationType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.lang.String.join;
import static orm.config.OrmConfig.ORM_CONFIG;
import static orm.config.RelationType.ONE_TO_MANY;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class Query {

    private Entity entity;
    private QueryConfig query = new QueryConfig();
    private boolean ignoreJoin;

    public static Query where(String column, String operator, Object input) {
        Query query = new Query();
        query.query.getWhere().add(Where.builder()
                .field(column)
                .operator(operator)
                .input(input)
                .build());
        return query;
    }

    public static <Any> List<Any> fetchAllFrom(Class<Any> type) {
        Query query = new Query();
        return query.fetchAll(type);
    }

    public Query and(String column, String operator, Object input) {
        query.getWhere().add(Where.builder()
                .field(column)
                .operator(operator)
                .input(input)
                .join("and")
                .build());
        return this;
    }

    public Query or(String column, String operator, Object input) {
        query.getWhere().add(Where.builder()
                .field(column)
                .operator(operator)
                .input(input)
                .join("or")
                .build());
        return this;
    }

    private Query ignoreJoin() {
        ignoreJoin = true;
        return this;
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <Any> List<Any> fetchAll(Class<Any> type) {
        Entity entity = ORM_CONFIG.get(type);
        this.entity = entity;
        query.setFrom(entity);
        mapSelect(entity.getFields(), entity.getTableName());
        if (!ignoreJoin) {
            entity.getRelations().values().stream()
                    .filter(it -> !ONE_TO_MANY.equals(it.getType()))
                    .forEach(relation -> {
                        var left = ORM_CONFIG.get(relation.getLeft());
                        var right = ORM_CONFIG.get(relation.getRight());
                        query.getJoin().add(Join.builder()
                                .left(left)
                                .right(right)
                                .type(JoinType.INNER_JOIN)
                                .build());
                        mapSelect(right.getFields(), right.getTableName());
                    });
        }
        return (List<Any>) execute(this::convertToEntity);
    }

    private void mapSelect(List<Field> fields, String tableName) {
        fields.forEach(field -> query.getSelect().put(String.format("%s.%s as %s_%s", tableName, field.getName(), tableName, field.getName()), field));
    }

    @SneakyThrows
    private Object convertToEntity(ResultSet rs) {
        List<Object> result = new ArrayList<>();
        while (rs.next()) {
            // instantiate "from" object with values
            Object o = entity.getType().getDeclaredConstructor().newInstance();
            for (Field f : entity.getFields()) {
                java.lang.reflect.Field declaredField = entity.getType().getDeclaredField(f.getName());
                declaredField.setAccessible(true);
                declaredField.set(o, rs.getObject(join("_", entity.getTableName(), f.getName())));
            }

            // instantiate object for joins if eager fetching is active
            // TODO: does not work for List Join (OneToMany)
            if (!ignoreJoin) {
                for (EntityRelation relation: entity.getRelations().values()) {
                    Entity joinEntity = ORM_CONFIG.get(relation.getRight());
                    if (relation.getType() == RelationType.MANY_TO_ONE) {
                        Object joinObject = relation.getRight().getDeclaredConstructor().newInstance();
                        for (Field f : joinEntity.getFields()) {
                            java.lang.reflect.Field declaredField = relation.getRight().getDeclaredField(f.getName());
                            declaredField.setAccessible(true);
                            declaredField.set(joinObject, rs.getObject(join("_", joinEntity.getTableName(), f.getName())));
                        }
                        // add join object to main object
                        java.lang.reflect.Field declaredField = entity.getType().getDeclaredField(relation.getFieldName());
                        declaredField.setAccessible(true);
                        declaredField.set(o, joinObject);
                    } else {
                        java.lang.reflect.Field fieldId = entity.getType().getDeclaredField(entity.getIdName());
                        fieldId.setAccessible(true);
                        List<?> joinObject = where(relation.getForeignKeyName(), "=", fieldId.get(o))
                                .ignoreJoin()
                                .fetchAll(joinEntity.getType());
                        java.lang.reflect.Field declaredField = entity.getType().getDeclaredField(relation.getFieldName());
                        declaredField.setAccessible(true);
                        declaredField.set(o, joinObject);
                    }
                }
            }

            result.add(o);
        }
        return result;
    }

    @SneakyThrows
    private Object execute(Function<ResultSet, ?> fn) {
        try (PreparedStatement preparedStatement = connection().prepareStatement(query.buildQuery())) {
            log.info(query.buildQuery());
            return fn.apply(preparedStatement.executeQuery());
        } catch (SQLException e) {
            String sqlQuery = query.buildQuery();
            throw e;
        }
    }

    private Connection connection() {
        return ConnectionContext.CONNECTION.get();
    }
}
