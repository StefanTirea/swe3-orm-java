package orm.config;

import lombok.Builder;
import lombok.Data;

import java.lang.annotation.Annotation;
import java.util.List;

@Data
@Builder
public class Field {

    private String name;
    private Class<?> type;
    private List<? extends Annotation> annotations;
}
