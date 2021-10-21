package orm.meta;

import lombok.Builder;
import lombok.Getter;
import orm.annotation.Table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class Entity {

    private String tableName;
    private Class<?> type;
    private List<Field> fields;

    public Entity(Class<?> type) {
        tableName = type.getAnnotation(Table.class).value();
        this.type = type;
        this.fields = Arrays.stream(type.getDeclaredFields())
                .map(field -> new Field(field, this))
                .collect(Collectors.toList());
    }
}
