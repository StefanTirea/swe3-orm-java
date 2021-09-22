package orm.sql;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import orm.config.Entity;
import orm.config.EntityRelation;
import orm.config.Field;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.nonNull;
import static org.apache.commons.lang3.StringUtils.joinWith;
import static orm.config.OrmConfig.ORM_CONFIG;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Query {

    private Entity entity;
    private QueryConfig query = new QueryConfig();

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

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public <Any> List<Any> fetchAll(Class<Any> type) {
        Entity entity = ORM_CONFIG.get(type);
        this.entity = entity;
        query.setFrom(entity);
        entity.getFields().forEach(field -> query.getSelect().put(joinWith(".", entity.getTableName(), field.getName()), field));
        entity.getRelations().values()
                .forEach(relation -> {
                    var left = ORM_CONFIG.get(relation.getLeft());
                    var right = ORM_CONFIG.get(relation.getRight());
                    query.getJoin().add(Join.builder()
                                    .left(left)
                                    .right(right)
                                    .type(JoinType.INNER_JOIN)
                            .build());
                    right.getFields().forEach(field -> query.getSelect().put(joinWith(".", right.getTableName(), field.getName()), field));
                });
        return (List<Any>) execute(this::convertToEntity);
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
                declaredField.set(o, rs.getObject(f.getName()));
            }

            // instantiate object for joins if eager fetching is active
            // TODO: does not work for List Join (OneToMany)
            for (EntityRelation relation: entity.getRelations().values()) {
                Object joinObject = relation.getRight().getDeclaredConstructor().newInstance();
                for (Field f : ORM_CONFIG.get(relation.getRight()).getFields()) {
                    java.lang.reflect.Field declaredField = relation.getRight().getDeclaredField(f.getName());
                    declaredField.setAccessible(true);
                    declaredField.set(joinObject, rs.getObject(f.getName()));
                }
                // add join object to main object
                java.lang.reflect.Field declaredField = entity.getType().getDeclaredField(relation.getFieldName());
                declaredField.setAccessible(true);
                declaredField.set(o, joinObject);
            }

            result.add(o);
        }
        return result;
    }

    @SneakyThrows
    private Object execute(Function<ResultSet, ?> fn) {
        try (PreparedStatement preparedStatement = connection().prepareStatement(query.buildQuery())) {
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
