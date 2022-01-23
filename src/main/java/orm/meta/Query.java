package orm.meta;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Query {

    private final List<Pair<String, Object>> where = new ArrayList<>();

    public static Query where(String column, Object columnValue) {
        Query query = new Query();
        query.where.add(Pair.of(column, columnValue));
        return query;
    }

    public Query and(String column, Object columnValue) {
        where.add(Pair.of(column, columnValue));
        return this;
    }

    protected List<Pair<String, Object>> build(Entity entity) {
        return where;
    }
}
