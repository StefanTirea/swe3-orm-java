package orm.meta;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Query {

    private final List<Pair<String, Object>> where = new ArrayList<>();
    private final List<Expression> expressions = new ArrayList<>();

    public static Expression where() {
        Query query = new Query();
        Expression expression = Expression.builder()
                .mainQuery(query)
                .build();
        query.expressions.add(expression);
        return expression;
    }

    public Expression and() {
        Expression expression = Expression.builder()
                .mainQuery(this)
                .andOr(" and")
                .build();
        expressions.add(expression);
        return expression;
    }

    public Expression or() {
        Expression expression = Expression.builder()
                .mainQuery(this)
                .andOr(" or")
                .build();
        expressions.add(expression);
        return expression;
    }

    protected Pair<String, List<Object>> build(Entity entity) {
        // optional TODO validate if column name + type is valid
        String sqlQuery = (expressions.isEmpty() ? "" : "WHERE") + expressions.stream()
                .map(this::mapWhereQuery)
                .collect(Collectors.joining());
        List<Object> values = expressions.stream()
                .flatMap(it -> it.getValue() instanceof List
                        ? ((List<?>) it.getValue()).stream()
                        : Stream.of(it.getValue()))
                .toList();
        return Pair.of(sqlQuery, values);
    }

    private String mapWhereQuery(Expression it) {
        if (it.getOperator().equals("in")) {
            String values = String.format("(%s)", ((List<?>) it.getValue()).stream()
                    .map(value -> "?")
                    .collect(Collectors.joining(",")));
            return String.format("%s%s %s %s %s", it.getNot(), it.getAndOr(), it.getColumn(), it.getOperator(), values);
        } else {
            return String.format("%s%s %s %s ?", it.getNot(), it.getAndOr(), it.getColumn(), it.getOperator());
        }
    }
}
