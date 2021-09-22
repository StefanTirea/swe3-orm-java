package orm.sql;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import orm.config.Field;

import java.lang.reflect.Array;
import java.util.List;

@Data
@Builder
public class Where {

    private String field;
    private String operator;
    private Object input;
    private String join;

    public String build() {
        return StringUtils.joinWith(" ", join, field, operator, mapInput()).trim();
    }

    private Object mapInput() {
        if (input == null) {
            return "null";
        } else if (input instanceof String) {
            return "'" + input + "'";
        } else if (input instanceof Iterable) {
            throw new UnsupportedOperationException("Array / Iterable not implemented yet");
        } else {
            return input;
        }
    }
}
