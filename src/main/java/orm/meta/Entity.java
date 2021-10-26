package orm.meta;

import lombok.Getter;
import orm.annotation.Table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;

@Getter
public class Entity {

    private final String tableName;
    private final Class<?> type;
    private final List<Field> columnFields;
    private final List<Field> otherFields;
    private final Field primaryKeyField;

    public Entity(Class<?> type) {
        this.tableName = type.getAnnotation(Table.class).value();
        this.type = type;

        List<Field> allFields = Arrays.stream(type.getDeclaredFields())
                .map(field -> new Field(field, this))
                .collect(Collectors.toList());
        this.columnFields = allFields.stream()
                .filter(field -> !(field.isPrimaryKey() || field.isVirtualColumn()))
                .collect(Collectors.toList());
        this.otherFields = allFields.stream()
                .filter(Field::isVirtualColumn)
                .collect(Collectors.toList());

        this.primaryKeyField = allFields.stream().filter(Field::isPrimaryKey).findFirst().orElseThrow();
    }

    public List<Object> getColumnValues(Object o) {
        return columnFields.stream()
                .map(field -> field.getColumnValue(o))
                .collect(Collectors.toList());
    }
}
