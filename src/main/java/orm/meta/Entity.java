package orm.meta;

import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import orm.annotation.Ignore;
import orm.annotation.SubEntity;
import orm.annotation.Table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
class Entity {

    private final String tableName;
    private final Class<?> type;
    private final List<Field> columnFields;
    private final List<Field> allFields;
    private final List<Field> virtualFields;
    private final Field primaryKeyField;

    public Entity(Class<?> type) {
        // TODO Validation
        validate(type);
        this.tableName = type.getAnnotation(Table.class).value();
        this.type = type;

        java.lang.reflect.Field[] fields = type.getDeclaredFields();
        if (type.getSuperclass().isAnnotationPresent(SubEntity.class)) {
            fields = ArrayUtils.addAll(fields, type.getSuperclass().getDeclaredFields());
        }
        this.allFields = Arrays.stream(fields)
                .filter(it -> !it.isAnnotationPresent(Ignore.class))
                .map(field -> new Field(field, this))
                .sorted((o1, o2) -> Boolean.compare(o2.isPrimaryKey(), o1.isPrimaryKey()))
                .collect(Collectors.toList());
        this.columnFields = allFields.stream()
                .filter(field -> !(field.isPrimaryKey() || field.isVirtualColumn()))
                .collect(Collectors.toList());
        this.virtualFields = allFields.stream()
                .filter(Field::isVirtualColumn)
                .collect(Collectors.toList());

        this.primaryKeyField = allFields.stream().filter(Field::isPrimaryKey).findFirst().orElseThrow();
    }

    public List<Object> getColumnValues(Object o) {
        return columnFields.stream()
                .map(field -> field.getColumnValue(o))
                .toList();
    }

    public List<Field> getForeignKeys() {
        return getColumnFields().stream()
                .filter(Field::isForeignKey)
                .toList();
    }

    private void validate(Class<?> type) {
        if (!type.isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("Table");
        }
    }

    @Override
    public String toString() {
        return String.format("%s[tableName=%s, type=%s]", this.getClass(), tableName, type);
    }
}
