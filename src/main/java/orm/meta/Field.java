package orm.meta;

import lombok.Getter;
import lombok.SneakyThrows;
import orm.annotation.Column;
import orm.annotation.Id;
import orm.annotation.ManyToMany;
import orm.annotation.ManyToOne;
import orm.annotation.OneToMany;
import orm.sample.LazyLoadingInterceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

@Getter
class Field {

    private final String columnName;
    private final Entity entity;
    private final boolean primaryKey;
    private final boolean foreignKey;
    /**
     * Column does not exist in DB Table and is used solely for automatic joins
     */
    private final boolean virtualColumn;
    private final boolean manyToMany;
    private final String manyToManyTable;
    private final boolean nullable;
    private final boolean lazy;
    private Class<?> type;
    private Class<?> subType;
    private Method method;
    private Method setMethod;

    @SneakyThrows
    public Field(java.lang.reflect.Field field, Entity entity) {
        this.entity = entity;
        this.columnName = mapColumnName(field);
        mapFieldTypes(field);
        mapMethods(field, entity);
        this.primaryKey = field.isAnnotationPresent(Id.class);
        this.foreignKey = field.isAnnotationPresent(ManyToOne.class);
        this.virtualColumn = isOneToManyOrManyToMany(field);
        this.manyToMany = field.isAnnotationPresent(ManyToMany.class);
        this.manyToManyTable = getAnnotation(field, ManyToMany.class).map(ManyToMany::tableName).orElse(null);
        this.nullable = mapNullable(field);
        this.lazy = getAnnotation(field, ManyToOne.class).map(ManyToOne::lazy).orElse(false)
                || getAnnotation(field, OneToMany.class).map(OneToMany::lazy).orElse(false)
                || getAnnotation(field, ManyToMany.class).map(ManyToMany::lazy).orElse(false);
    }

    /**
     * @param entity Object of a {@link Entity}
     * @return column of this field and if it is a foreign key then the primary key value
     */
    @SneakyThrows
    public Object getColumnValue(Object entity) {
        var columnValue = getMethod().invoke(entity);
        return isForeignKey()
                ? new Entity(getType()).getPrimaryKeyField().getMethod().invoke(columnValue)
                : columnValue;
    }

    @SneakyThrows
    public Object invokeGetMethod(Object entity) {
        return getMethod().invoke(entity);
    }

    private String mapColumnName(java.lang.reflect.Field field) {
        if (field.isAnnotationPresent(OneToMany.class)) {
            return field.getAnnotation(OneToMany.class).foreignKeyName();
        } else if (field.isAnnotationPresent(ManyToOne.class)) {
            return field.getAnnotation(ManyToOne.class).foreignKeyName();
        } else if (field.isAnnotationPresent(ManyToMany.class)) {
            return field.getAnnotation(ManyToMany.class).foreignKeyName();
        } else {
            return field.getName();
        }
    }

    /**
     * Maps the Entity column type.
     * <p>
     * If column is either {@link OneToMany} or {@link ManyToMany} then the generic subtype is mapped instead.
     * This is required because the actual type will always be {@link Collection}.
     *
     * @param field of Entity column
     */
    private void mapFieldTypes(java.lang.reflect.Field field) {
        this.type = field.getType();
        if (isOneToManyOrManyToMany(field) && Arrays.asList(type.getInterfaces()).contains(Collection.class)) {
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            this.subType = (Class<?>) genericType.getActualTypeArguments()[0];
        }
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

    private boolean isOneToManyOrManyToMany(java.lang.reflect.Field field) {
        return field.isAnnotationPresent(OneToMany.class) || field.isAnnotationPresent(ManyToMany.class);
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
