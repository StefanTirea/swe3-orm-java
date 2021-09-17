package orm.config;

import lombok.Builder;
import lombok.Data;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class Entity {

    private String tableName;
    private String idName;
    private Class<?> type;
    private List<Field> fields;
    private List<? extends Annotation> annotations;
    private Map<Class<?>, EntityRelation> relations;
}
