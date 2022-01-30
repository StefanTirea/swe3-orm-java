package orm.meta;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
class QueryResult {

    private String whereQuery;
    @Singular
    private List<Object> values;

    static QueryResult empty() {
        return QueryResult.builder().whereQuery("").build();
    }
}
