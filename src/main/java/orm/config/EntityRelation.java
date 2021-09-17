package orm.config;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EntityRelation {

    private Class<?> left;
    private Class<?> right;
    private RelationType type;
}
