package orm.meta;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter(AccessLevel.PROTECTED)
public class Expression {

    private final Query mainQuery;
    private final String andOr;
    private String column;
    private String operator;
    private Object value;
    private String not = "";

    @Builder(access = AccessLevel.PACKAGE)
    public Expression(Query mainQuery, String andOr) {
        this.mainQuery = mainQuery;
        this.andOr = andOr == null ? "" : andOr;
    }

    public Expression not() {
        this.not = " not";
        return this;
    }

    public Query equals(String column, Object columnValue) {
        setValue(column, columnValue, "=");
        return mainQuery;
    }

    public Query like(String column, Object columnValue) {
        setValue(column, columnValue, "like");
        return mainQuery;
    }

    public Query greaterThan(String column, Object columnValue) {
        setValue(column, columnValue, ">");
        return mainQuery;
    }

    public Query lessThan(String column, Object columnValue) {
        setValue(column, columnValue, "<");
        return mainQuery;
    }

    public Query greaterOrEqual(String column, Object columnValue) {
        setValue(column, columnValue, ">=");
        return mainQuery;
    }

    public Query lessOrEqual(String column, Object columnValue) {
        setValue(column, columnValue, "<=");
        return mainQuery;
    }

    public Query in(String column, List<Object> columnValue) {
        setValue(column, columnValue, "in");
        return mainQuery;
    }

    public Query in(String column, Object... columnValue) {
        setValue(column, Arrays.asList(columnValue), "in");
        return mainQuery;
    }

    private void setValue(String column, Object columnValue, String operator) {
        this.column = column;
        this.value = columnValue;
        this.operator = operator;
    }
}
