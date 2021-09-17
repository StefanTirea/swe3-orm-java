package orm.sql;

import lombok.Builder;
import lombok.Data;
import orm.config.Field;

@Data
@Builder
public class Where {

    private Field field;
    private String operator;
    private String input;

    public String build() {
        return field.getName() + operator + input;
    }
}
