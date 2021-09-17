package orm;

import lombok.Data;
import org.reflections.Reflections;
import orm.annotation.Id;
import orm.annotation.OneToMany;
import orm.annotation.Relation;
import orm.annotation.Table;
import orm.config.Entity;
import orm.config.EntityRelation;
import orm.config.Field;
import orm.config.OrmConfig;
import orm.config.RelationType;

import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static java.util.function.Function.identity;
import static java.util.function.Predicate.not;
import static orm.config.RelationType.MANY_TO_ONE;
import static orm.config.RelationType.ONE_TO_MANY;

@Data
public class ConfigGenerator {

    public ConfigGenerator() {
        generateConfig();
    }

    public void generateConfig() {
        Reflections reflections = new Reflections("");
        Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(Table.class);
        typesAnnotatedWith.forEach(clazz -> OrmConfig.ORM_CONFIG.put(clazz, mapEntity(clazz)));
    }

    private EntityRelation mapEntityRelation(Class<?> clazz, java.lang.reflect.Field field) {
        return EntityRelation.builder()
                .left(clazz)
                .right((Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0])
                .type(field.isAnnotationPresent(OneToMany.class) ? ONE_TO_MANY : MANY_TO_ONE)
                .build();
    }

    private Entity mapEntity(Class<?> clazz) {
        return Entity.builder()
                .tableName(clazz.getAnnotation(Table.class).value())
                .idName(Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Id.class)).findFirst().orElseThrow().getName())
                .type(clazz)
                .fields(Arrays.stream(clazz.getDeclaredFields())
                        .filter(field -> Arrays.stream(field.getDeclaredAnnotations()).noneMatch(it -> it.annotationType().isAnnotationPresent(Relation.class)))
                        .map(this::mapField)
                        .collect(Collectors.toList()))
                .annotations(Arrays.asList(clazz.getDeclaredAnnotations()))
                .relations(Arrays.stream(clazz.getDeclaredFields())
                        .filter(field -> Arrays.stream(field.getDeclaredAnnotations()).anyMatch(it -> it.annotationType().isAnnotationPresent(Relation.class)))
                        .map(field -> mapEntityRelation(clazz, field))
                        .collect(Collectors.toMap(EntityRelation::getRight, identity())))
                .build();
    }

    private Field mapField(java.lang.reflect.Field field) {
        return Field.builder()
                .type(field.getType())
                .name(field.getName())
                .annotations(Arrays.asList(field.getDeclaredAnnotations()))
                .build();
    }
}
