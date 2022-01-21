package orm.meta;

import lombok.Getter;
import lombok.SneakyThrows;
import orm.annotation.Column;
import orm.annotation.Id;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Getter
class Field {

    private final String columnName;
    private final Class<?> type;
    private final Entity entity;

    private final boolean primaryKey;
    private final boolean foreignKey;
    /**
     * Column does not exist and is used solely for joins
     */
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
        this.type = field.isAnnotationPresent(OneToMany.class) ? field.getAnnotation(OneToMany.class).columnType() : field.getType();
        mapMethods(field, entity);
        this.primaryKey = field.isAnnotationPresent(Id.class);
        this.foreignKey = field.isAnnotationPresent(ManyToOne.class);
        this.virtualColumn = field.isAnnotationPresent(OneToMany.class);
        this.nullable = mapNullable(field);
        this.lazy = getAnnotation(field, ManyToOne.class).map(ManyToOne::lazy).orElse(false)
                || getAnnotation(field, OneToMany.class).map(OneToMany::lazy).orElse(false);
    }

    /**
     * @param entity Object of a {@link Entity}
     * @return column of this field and if it is a foreign key object then the primary key value
     */
    @SneakyThrows
    public Object getColumnValue(Object entity) {
        var columnValue = getMethod().invoke(entity);
        return isForeignKey()
                ? new Entity(getType()).getPrimaryKeyField().getMethod().invoke(columnValue)
                : columnValue;
    }

    private void mapMethods(java.lang.reflect.Field field, Entity entity) {
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

    private <T extends Annotation> Optional<T> getAnnotation(java.lang.reflect.Field field, Class<T> annotationClass) {
        return field.isAnnotationPresent(annotationClass)
                ? Optional.of(field.getAnnotation(annotationClass))
                : Optional.empty();
    }
}
