package orm.sql;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
public class Where {

    private String field;
    private String operator;
    private Object input;
    private String join;

    public String build() {
        return StringUtils.joinWith(" ", join, field, operator, mapInput());
    }

    // TODO handle types correctly
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
