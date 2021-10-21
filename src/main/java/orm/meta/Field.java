package orm.meta;

import lombok.Builder;
import lombok.Getter;
import lombok.SneakyThrows;
import orm.annotation.Column;
import orm.annotation.Id;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;

import java.lang.reflect.Method;
import java.util.Arrays;

@Getter
@Builder
public class Field {

    private String columnName;
    private Class<?> type;
    private Entity entity;

    private boolean primaryKey;
    private boolean foreignKey;
    private boolean nullable;

    private Method method;
    private Method setMethod;

    @SneakyThrows
    public Field(java.lang.reflect.Field field, Entity entity) {
        this.entity = entity;
        type = field.getType();
        findMethods(field, entity);
        primaryKey = field.isAnnotationPresent(Id.class);
        foreignKey = field.isAnnotationPresent(OneToMany.class);
        nullable = mapNullable(field);
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
        return (field.isAnnotationPresent(Column.class) || field.getAnnotation(Column.class).nullable())
                || (field.isAnnotationPresent(OneToMany.class) || field.getAnnotation(OneToMany.class).nullable())
                || (field.isAnnotationPresent(ManyToOne.class) || field.getAnnotation(ManyToOne.class).nullable());
    }
}
