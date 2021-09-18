package orm.sql;

import java.sql.Connection;
import java.util.List;

import static orm.sql.Query.selectFrom;

@orm.annotation.Repository
public abstract class Repository<T> {

    private final Class<T> type;

    public Repository(Class<T> type) {
        this.type = type;
    }

    public List<T> selectAll() {
        return selectFrom(type).fetchAll();
    }

    public Query<T> select(String... columns) {
        return Query.select(columns);
    }

    private Connection connection() {
        return ConnectionContext.CONNECTION.get();
    }
}
