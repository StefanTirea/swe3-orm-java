package orm.meta;

import lombok.Getter;
import lombok.SneakyThrows;
import orm.annotation.Column;
import orm.annotation.Id;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
public class Field {

    private final String columnName;
    private final Class<?> type;
    private final Entity entity;

    private final boolean primaryKey;
    private final boolean foreignKey;
    private final boolean virtualColumn;
    private final boolean nullable;
    private final boolean lazy;

    private Method method;
    private Method setMethod;

    @SneakyThrows
    public Field(java.lang.reflect.Field field, Entity entity) {
        this.entity = entity;
        this.columnName = field.isAnnotationPresent(ManyToOne.class) && isNotBlank(field.getAnnotation(ManyToOne.class).value())
                ? field.getAnnotation(ManyToOne.class).value()
                : field.getName();
        this.type = field.getType();
        findMethods(field, entity);
        this.primaryKey = field.isAnnotationPresent(Id.class);
        this.foreignKey = field.isAnnotationPresent(ManyToOne.class);
        this.virtualColumn = field.isAnnotationPresent(OneToMany.class);
        this.nullable = mapNullable(field);
        this.lazy = true; // TODO
    }

    /**
     *
     * @param o Object value of the #Entity
     * @return object value of the field and if it is an Entity object then the primary key value
     */
    @SneakyThrows
    public Object getColumnValue(Object o) {
        if (isForeignKey()) {
            var foreignKeyObject = getMethod().invoke(o);
            return new Entity(getType()).getPrimaryKeyField().getMethod().invoke(foreignKeyObject);
        }
        return getMethod().invoke(o);
    }

    private void findMethods(java.lang.reflect.Field field, Entity entity) {
        Arrays.stream(entity.getType().getDeclaredMethods())
                .forEach(method -> {
                    if (method.getName().equalsIgnoreCase("get" + field.getName())) {
                        this.method = method;
                    }
                    if (method.getName().equalsIgnoreCase("set" + field.getName())) {
                        this.setMethod = method;
                    }
                });
        if (method == null || setMethod == null) {
            throw new IllegalStateException(String.format("get or set Method not found for %s %s", entity, field));
        }
    }

    private boolean mapNullable(java.lang.reflect.Field field) {
        return (field.isAnnotationPresent(Column.class) && field.getAnnotation(Column.class).nullable())
                || (field.isAnnotationPresent(ManyToOne.class) && field.getAnnotation(ManyToOne.class).nullable());
    }
}
