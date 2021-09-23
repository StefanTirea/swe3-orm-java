package orm.sql;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import orm.config.Entity;
import orm.config.Field;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Data
public class QueryConfig {

    private Map<String, Field> select = new HashMap<>();
    private Entity from;
    private List<Where> where = new ArrayList<>();
    private List<Join> join = new ArrayList<>();

    public String buildQuery() {
        return "select " + select.entrySet().stream().map(Map.Entry::getKey).collect(Collectors.joining(","))
                + "\nfrom " + from.getTableName()
                + "\n" + join.stream().map(this::mapJoin).collect(Collectors.joining("\n"))
                + mapWhereClause()
                + ";";
    }

    private String mapWhereClause() {
        if (!where.isEmpty()) {
            return "\nwhere " + where.stream().
                    map(Where::build)
                    .collect(Collectors.joining());
        }
        return "";
    }

    private String mapJoin(Join join) {
        String foreignKeyLeft = join.getRight().getRelations().get(join.getLeft().getType()).getForeignKeyName();
        String foreignKeyRight = join.getLeft().getRelations().get(join.getRight().getType()).getForeignKeyName();
        String leftJoinId = nonNull(foreignKeyLeft) ? foreignKeyLeft : join.getLeft().getIdName();
        String rightJoinId = nonNull(foreignKeyRight) ? foreignKeyRight : join.getRight().getIdName();
        join.validate();
        return join.getType().getSql() + join.getRight().getTableName() + String.format(" ON %s.%s = %s.%s", join.getLeft().getTableName(), leftJoinId,
                join.getRight().getTableName(), rightJoinId);
    }
}

@Data
@Builder
class Join {

    private Entity left;
    private Entity right;
    private JoinType type;

    public void validate() {
        boolean a1 = left.getRelations().containsKey(right.getType());
        boolean a2 = right.getRelations().containsKey(left.getType());
        if (!(a1 || a2)) {
            throw new IllegalArgumentException();
        }
    }
}

@RequiredArgsConstructor
@Getter
enum JoinType {
    INNER_JOIN("INNER JOIN "),
    LEFT_JOIN("LEFT JOIN "),
    RIGHT_JOIN("RIGHT JOIN "),
    ;

    private final String sql;
}
